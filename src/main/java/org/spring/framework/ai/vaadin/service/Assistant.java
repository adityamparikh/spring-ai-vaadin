package org.spring.framework.ai.vaadin.service;

import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.Nullable;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springaicommunity.mcp.security.client.sync.AuthenticationMcpTransportContextProvider;
import reactor.core.publisher.Flux;

/**
 * Core AI assistant service that handles chat interactions with language models.
 *
 * <p>This service provides:
 * <ul>
 *   <li>Streaming chat responses using Spring AI's ChatClient</li>
 *   <li>Retrieval-Augmented Generation (RAG) with vector store integration</li>
 *   <li>Chat memory for conversation context</li>
 *   <li>File attachment processing (images, PDFs, text files)</li>
 *   <li>Optional MCP (Model Context Protocol) tool integration</li>
 *   <li>Content safety filtering via SafeGuardAdvisor</li>
 * </ul>
 *
 * <p>The assistant uses a RAG pipeline that rewrites queries for better search results
 * and augments prompts with relevant context from the vector store.
 *
 * @see RagContextService for managing RAG data sources
 */
@Service
public class Assistant {

  /**
   * Configuration options for chat interactions.
   *
   * @param systemMessage custom system prompt to guide AI behavior
   * @param useMcp whether to enable MCP tool calling capabilities
   */
  public record ChatOptions(String systemMessage, boolean useMcp) {}

  private final ChatOptions defaultOptions = new ChatOptions("", false);

  private final ChatClient chatClient;
  private final ChatMemory chatMemory;
  private final List<McpSyncClient> mcpSyncClients;

  private static final String DEFAULT_SYSTEM =
      """
        You are an expert on all things Java and Spring related.
        Answer questions in a friendly manner and give clear explanations.
        Always give example code snippets when explaining code.
        """;

  private static final String ATTACHMENT_TEMPLATE =
      """
        <attachment filename="%s">
                %s
        </attachment>
        """;

  /**
   * Creates a new Assistant with the specified dependencies.
   *
   * @param chatMemory the chat memory store for conversation history
   * @param builder the ChatClient builder for AI model interactions
   * @param vectorStore the vector store for RAG document retrieval
   * @param mcpSyncClients list of MCP clients for tool integrations
   */
  public Assistant(
      ChatMemory chatMemory,
      ChatClient.Builder builder,
      VectorStore vectorStore,
      List<McpSyncClient> mcpSyncClients) {
    this.chatMemory = chatMemory;
    this.mcpSyncClients = mcpSyncClients;

    chatClient =
        builder
            .defaultAdvisors(

                // Absolutely don't let people ask about PHP 😆
                new SafeGuardAdvisor(List.of("PHP")),

                // Remember the conversation
                MessageChatMemoryAdvisor.builder(chatMemory).build(),

                // Define RAG pipeline
                // See
                // https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html#modules
                RetrievalAugmentationAdvisor.builder()
                    .queryTransformers(
                        // Rewrite the query for better search results
                        RewriteQueryTransformer.builder()
                            .chatClientBuilder(builder.build().mutate())
                            .build())
                    // Allow empty context (so you can try the assistant without context and
                    // compare)
                    .queryAugmenter(
                        ContextualQueryAugmenter.builder().allowEmptyContext(true).build())

                    // Use the vector store to retrieve documents
                    .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                            .similarityThreshold(0.50)
                            .vectorStore(vectorStore)
                            .build())
                    .build())
            .build();
  }

  /**
   * Streams AI responses for a chat message.
   *
   * <p>Processes the user message along with any attachments and streams the AI response
   * token by token. The response includes RAG context from the vector store when relevant
   * documents are found.
   *
   * @param chatId unique identifier for the chat session
   * @param userMessage the user's message text
   * @param attachments list of file attachments (images, PDFs, text files)
   * @param options optional chat configuration (system message, MCP usage)
   * @return a Flux of response tokens that can be subscribed to for streaming
   */
  public Flux<String> stream(
      String chatId,
      String userMessage,
      List<AttachmentFile> attachments,
      @Nullable ChatOptions options) {
    if (options == null) {
      options = defaultOptions;
    }

    var system = options.systemMessage().isBlank() ? DEFAULT_SYSTEM : options.systemMessage();

    var processedAttachments = processAttachments(attachments);

    var prompt =
        chatClient
            .prompt()
            .system(system)
            .user(
                u -> {
                  u.text(userMessage + processedAttachments.documentContent());
                  u.media(processedAttachments.mediaList().toArray(Media[]::new));
                })
            .advisors(
                a -> {
                  a.param(ChatMemory.CONVERSATION_ID, chatId);
                });

    if (options.useMcp) {
      prompt.toolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients));
    }

    return prompt
            .stream()
            .content()
            .contextWrite(AuthenticationMcpTransportContextProvider.writeToReactorContext());
  }

  /**
   * Retrieves the chat history for a given session.
   *
   * @param chatId the chat session identifier
   * @return list of messages from the conversation (user and assistant messages only)
   */
  public List<Message> getHistory(String chatId) {
    return chatMemory.get(chatId).stream()
        .filter(
            message ->
                message.getMessageType().equals(MessageType.USER)
                    || message.getMessageType().equals(MessageType.ASSISTANT))
        // TODO: Add attachments
        .map(
            message ->
                new Message(
                    message.getMessageType().toString().toLowerCase(),
                    message.getText(),
                    List.of()))
        .toList();
  }

  /**
   * Closes a chat session and clears its memory.
   *
   * @param chatId the chat session identifier to close
   */
  public void closeChat(String chatId) {
    chatMemory.clear(chatId);
  }

  private record ProcessedAttachments(String documentContent, List<Media> mediaList) {}

  private ProcessedAttachments processAttachments(List<AttachmentFile> attachments) {
    // Map text and pdf attachments as documents wrapped in <attachment> tags
    var documentList =
        attachments.stream()
            .filter(
                attachment ->
                    attachment.contentType().contains("text")
                        || attachment.contentType().contains("pdf"))
            .toList();

    var documentBuilder = new StringBuilder("\n");
    documentList.forEach(
        attachment -> {
          var data = new ByteArrayResource(attachment.data());
          var documents = new TikaDocumentReader(data).read();
          var content = String.join("\n", documents.stream().map(Document::getText).toList());
          documentBuilder.append(
              String.format(ATTACHMENT_TEMPLATE, attachment.fileName(), content));
        });

    // Map image attachments to Media objects
    var mediaList =
        attachments.stream()
            .filter(attachment -> attachment.contentType().contains("image"))
            .map(
                attachment ->
                    new Media(
                        MimeType.valueOf(attachment.contentType()),
                        new ByteArrayResource(attachment.data())))
            .toList();

    return new ProcessedAttachments(documentBuilder.toString(), mediaList);
  }

  /**
   * Represents a file attachment in a chat message.
   *
   * @param type the MIME type of the attachment
   * @param key unique identifier for the attachment
   * @param fileName the original filename
   * @param url URL or data URI for displaying the attachment
   */
  public static record Attachment(String type, String key, String fileName, String url) {}

  /**
   * Represents a chat message with its role, content, and attachments.
   *
   * @param role the message role ("user" or "assistant")
   * @param content the text content of the message
   * @param attachments optional list of file attachments
   */
  public static record Message(
      String role, String content, @Nullable List<Attachment> attachments) {}
}
