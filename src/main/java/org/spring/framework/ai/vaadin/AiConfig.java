package org.spring.framework.ai.vaadin;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Spring AI components.
 *
 * <p>This configuration sets up the necessary beans for AI-powered features,
 * including vector storage for Retrieval-Augmented Generation (RAG).
 */
@Configuration
public class AiConfig {

  /**
   * Creates an in-memory vector store for document embeddings.
   *
   * <p>The vector store is used by the RAG pipeline to store and retrieve
   * document embeddings for context-aware AI responses.
   *
   * @param embeddingModel the embedding model used to generate vector representations
   * @return a configured {@link SimpleVectorStore} instance
   */
  @Bean
  public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return SimpleVectorStore.builder(embeddingModel).build();
  }
}
