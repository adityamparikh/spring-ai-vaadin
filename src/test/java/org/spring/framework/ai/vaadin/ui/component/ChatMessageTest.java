package org.spring.framework.ai.vaadin.ui.component;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.spring.framework.ai.vaadin.ui.component.ChatMessage.ChatAttachment;

class ChatMessageTest {

  @Test
  void attachmentsAreNotAffectedByLaterChangesToTheCallersList() {
    var pending = new ArrayList<ChatAttachment>();
    pending.add(new ChatAttachment("text/plain", "notes.txt", new byte[] {1}, ""));

    var message = new ChatMessage("User", "hi", pending);
    pending.clear();

    assertThat(message.getAttachments()).extracting(ChatAttachment::fileName)
        .containsExactly("notes.txt");
  }

  @Test
  void messageWithoutContentShowsTypingIndicatorAndHasNoAttachments() {
    var message = new ChatMessage("Assistant", null, null);

    assertThat(message.getText()).contains("typing-indicator");
    assertThat(message.getAttachments()).isEmpty();
  }

  @Test
  void appendTextReplacesTypingIndicator() {
    var message = new ChatMessage("Assistant", null, null);

    message.appendText("Hello");
    message.appendText(" world");

    assertThat(message.getText()).isEqualTo("Hello world");
    assertThat(message.getRole()).isEqualTo("Assistant");
  }
}
