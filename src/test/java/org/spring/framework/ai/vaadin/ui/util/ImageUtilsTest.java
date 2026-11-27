package org.spring.framework.ai.vaadin.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageUtilsTest {

  @Test
  void createsThumbnailOfRequestedSize() throws IOException {
    var png = pngBytes(400, 300);

    var thumbnail = ImageUtils.createThumbnail(png, "image/png", 160, 140);

    var image = ImageIO.read(new ByteArrayInputStream(thumbnail));
    assertThat(image.getWidth()).isEqualTo(160);
    assertThat(image.getHeight()).isEqualTo(140);
  }

  @Test
  void returnsOriginalDataWhenImageCannotBeDecoded() {
    var notAnImage = "<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes();

    var thumbnail = ImageUtils.createThumbnail(notAnImage, "image/svg+xml", 160, 140);

    assertThat(thumbnail).isSameAs(notAnImage);
  }

  @Test
  void returnsOriginalDataWhenNoWriterExistsForFormat() throws IOException {
    var png = pngBytes(10, 10);

    var thumbnail = ImageUtils.createThumbnail(png, "image/x-no-such-format", 160, 140);

    assertThat(thumbnail).isSameAs(png);
  }

  private static byte[] pngBytes(int width, int height) throws IOException {
    var out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
    return out.toByteArray();
  }
}
