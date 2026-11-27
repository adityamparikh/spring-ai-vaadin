package org.spring.framework.ai.vaadin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.spring.framework.ai.vaadin.ui.util.CustomMultipartFile;
import org.springframework.ai.vectorstore.VectorStore;

class RagContextServiceTest {

  private final VectorStore vectorStore = mock(VectorStore.class);
  private final RagContextService service = new RagContextService(vectorStore);

  @Test
  void addFileToContextWritesDocumentsAndRecordsFileName() throws IOException {
    service.addFileToContext(textFile("notes.txt"));

    verify(vectorStore).write(anyList());
    assertThat(service.getFilesInContext()).containsExactly("notes.txt");
  }

  @Test
  void filesInContextCannotBeModifiedByCallers() throws IOException {
    service.addFileToContext(textFile("notes.txt"));

    var files = service.getFilesInContext();

    assertThatThrownBy(files::clear).isInstanceOf(UnsupportedOperationException.class);
    assertThat(service.getFilesInContext()).containsExactly("notes.txt");
  }

  private static CustomMultipartFile textFile(String name) {
    return new CustomMultipartFile(
        name, "text/plain", "Spring AI makes RAG easy.".getBytes(StandardCharsets.UTF_8));
  }
}
