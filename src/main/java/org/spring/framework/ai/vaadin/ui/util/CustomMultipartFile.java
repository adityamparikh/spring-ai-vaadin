package org.spring.framework.ai.vaadin.ui.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * A simple in-memory implementation of Spring's {@link MultipartFile} interface.
 *
 * <p>This class wraps byte array content to provide a {@link MultipartFile} compatible
 * interface, enabling Vaadin's upload components to work with Spring's file processing
 * utilities like the {@link org.spring.framework.ai.vaadin.service.RagContextService}.
 *
 * <p>Note: The {@link #transferTo(File)} method is not supported as this implementation
 * is designed for in-memory processing only.
 */
public class CustomMultipartFile implements MultipartFile {
  private final String name;
  private final String contentType;
  private final byte[] content;

  /**
   * Creates a new CustomMultipartFile with the specified attributes.
   *
   * @param name the filename
   * @param contentType the MIME content type
   * @param content the file content as a byte array
   */
  public CustomMultipartFile(String name, String contentType, byte[] content) {
    this.name = name;
    this.contentType = contentType;
    this.content = content;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOriginalFilename() {
    return name;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public boolean isEmpty() {
    return content == null || content.length == 0;
  }

  @Override
  public long getSize() {
    return content.length;
  }

  @Override
  public byte[] getBytes() throws IOException {
    return content;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(content);
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    throw new UnsupportedOperationException("Transfer to file not supported");
  }
}
