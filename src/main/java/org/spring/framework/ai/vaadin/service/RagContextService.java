package org.spring.framework.ai.vaadin.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for managing contextual data for Retrieval-Augmented Generation (RAG) processes.
 * This service allows the addition of file data to a context that can be utilized for
 * document retrieval and text processing operations.
 *
 * Files are managed in an in-memory list for simplicity.
 */
@Service
public class RagContextService {

  private final VectorStore vectorStore;
  private final List<String> filesInContext = new ArrayList<>();

  /**
   * Creates a new RAG context service with the specified vector store.
   *
   * @param vectorStore the vector store for storing document embeddings
   */
  public RagContextService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  /**
   * Adds a file to the RAG context by processing it and storing its embeddings.
   *
   * <p>The file is read using Apache Tika for content extraction, split into
   * tokens using a text splitter, and then stored in the vector store for
   * later retrieval during chat interactions.
   *
   * @param file the file to add to the context
   * @throws IOException if the file cannot be read
   */
  public void addFileToContext(MultipartFile file) throws IOException {
    var resource = new InputStreamResource(file.getInputStream());
    vectorStore.write(new TokenTextSplitter().apply(new TikaDocumentReader(resource).read()));

    filesInContext.add(file.getOriginalFilename());
  }

  /**
   * Returns the list of filenames currently loaded in the RAG context.
   *
   * @return list of filenames that have been added to the context
   */
  public List<String> getFilesInContext() {
    return filesInContext;
  }
}
