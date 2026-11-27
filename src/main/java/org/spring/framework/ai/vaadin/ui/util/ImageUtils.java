package org.spring.framework.ai.vaadin.ui.util;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for image operations. */
public final class ImageUtils {

  private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

  private ImageUtils() {}

  /**
   * Creates a thumbnail from the provided image data.
   *
   * @param imageData The original image data as byte array
   * @param contentType The content type of the image (e.g., "image/jpeg", "image/png")
   * @param thumbWidth The width of the thumbnail
   * @param thumbHeight The height of the thumbnail
   * @return The thumbnail as byte array, or the original data if thumbnail creation fails
   */
  public static byte[] createThumbnail(
      byte[] imageData, String contentType, int thumbWidth, int thumbHeight) {
    try {
      var bis = new ByteArrayInputStream(imageData);
      var originalImage = ImageIO.read(bis);
      if (originalImage == null) {
        // No ImageIO reader for this format (e.g. SVG, WebP)
        log.debug("Cannot decode {} image, using original data as thumbnail", contentType);
        return imageData;
      }

      // Create thumbnail image
      var thumbnailImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
      var g = thumbnailImage.createGraphics();
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
      g.dispose();

      // Convert thumbnail to byte array
      var bos = new ByteArrayOutputStream();
      // Extract format from contentType (e.g., "jpeg" from "image/jpeg")
      var format = contentType.substring(6);
      if (!ImageIO.write(thumbnailImage, format, bos)) {
        // No ImageIO writer for this format
        log.debug("Cannot encode {} image, using original data as thumbnail", contentType);
        return imageData;
      }
      return bos.toByteArray();
    } catch (IOException e) {
      // Fallback to original image if thumbnail creation fails
      log.warn("Failed to create thumbnail for {} image", contentType, e);
      return imageData;
    }
  }
}
