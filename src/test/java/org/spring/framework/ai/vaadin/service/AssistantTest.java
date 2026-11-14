package org.spring.framework.ai.vaadin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

/**
 * Drives the {@link Assistant} through the session advisors it is built with, against a stub chat
 * model and a real in-memory {@link SessionService}, so the assertions see what the session
 * actually stores and replays.
 */
class AssistantTest {

  private final SessionService sessionService =
      DefaultSessionService.builder()
          .sessionRepository(InMemorySessionRepository.builder().build())
          .build();

  private final ChatModel chatModel = mock(ChatModel.class);

  private Assistant assistant;

  @BeforeEach
  void setUp() {
    // ChatClient builds every request's options from the model's own.
    when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
    // The RAG pipeline's query rewriter calls the model; the vector store finds nothing.
    when(chatModel.call(any(Prompt.class))).thenReturn(response("rewritten query"));
    VectorStore vectorStore = mock(VectorStore.class);
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    assistant =
        new Assistant(sessionService, ChatClient.builder(chatModel), vectorStore, List.of());
  }

  @Test
  void keepsEachChatsTurnsInItsOwnSession() {
    when(chatModel.stream(any(Prompt.class)))
        .thenReturn(Flux.just(response("Hello, Alice.")))
        .thenReturn(Flux.just(response("Hello, Bob.")));

    send("chat-1", "My name is Alice.");
    send("chat-2", "My name is Bob.");

    assertThat(assistant.getHistory("chat-1"))
        .extracting(Assistant.Message::role)
        .containsExactly("user", "assistant");
    assertThat(assistant.getHistory("chat-1"))
        .extracting(Assistant.Message::content)
        .anySatisfy(content -> assertThat(content).contains("My name is Alice."))
        .anySatisfy(content -> assertThat(content).isEqualTo("Hello, Alice."))
        .noneSatisfy(content -> assertThat(content).contains("Bob"));
  }

  @Test
  void replaysEarlierTurnsOfTheSameChatToTheModel() {
    when(chatModel.stream(any(Prompt.class)))
        .thenReturn(Flux.just(response("Nice to meet you, Alice.")))
        .thenReturn(Flux.just(response("Your name is Alice.")));

    send("chat-1", "My name is Alice.");
    send("chat-1", "What is my name?");

    ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
    verify(chatModel, times(2)).stream(prompts.capture());
    assertThat(prompts.getAllValues().get(1).getInstructions())
        .extracting(Message::getText)
        .anySatisfy(text -> assertThat(text).contains("My name is Alice."))
        .anySatisfy(text -> assertThat(text).isEqualTo("Nice to meet you, Alice."));
  }

  @Test
  void closingAChatDeletesItsHistory() {
    when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(response("Hi.")));
    send("chat-1", "Hello.");

    assistant.closeChat("chat-1");

    assertThat(assistant.getHistory("chat-1")).isEmpty();
  }

  private void send(String chatId, String message) {
    assistant.stream(chatId, message, List.of(), null).collectList().block();
  }

  private static ChatResponse response(String text) {
    return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
  }
}
