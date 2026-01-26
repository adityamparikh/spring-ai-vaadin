package org.spring.framework.ai.vaadin.service;

/**
 * Represents an uploaded file attachment for chat messages.
 *
 * <p>This record holds the file metadata and binary content for attachments
 * that are processed by the {@link Assistant} service. Supported content types
 * include images, PDFs, and text files.
 *
 * @param fileName the original filename of the attachment
 * @param contentType the MIME content type (e.g., "image/png", "application/pdf", "text/plain")
 * @param data the raw binary content of the file
 */
public record AttachmentFile(String fileName, String contentType, byte[] data) {}
