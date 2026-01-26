# Vaadin Guide for This Application

This guide teaches you Vaadin concepts specifically used in this Spring AI chat application. It's designed for developers who know Java but are new to Vaadin.

## Table of Contents

1. [What is Vaadin?](#what-is-vaadin)
2. [How Vaadin Works](#how-vaadin-works)
3. [Project Structure](#project-structure)
4. [Core Concepts](#core-concepts)
5. [Components Used in This App](#components-used-in-this-app)
6. [Layouts](#layouts)
7. [Routing](#routing)
8. [Event Handling](#event-handling)
9. [Server Push (Real-time Updates)](#server-push-real-time-updates)
10. [Styling and Themes](#styling-and-themes)
11. [Spring Integration](#spring-integration)
12. [Code Walkthrough](#code-walkthrough)

---

## What is Vaadin?

Vaadin is a **server-side Java UI framework**. Unlike React or Angular where you write JavaScript/TypeScript for the frontend, in Vaadin you write **only Java**. Vaadin automatically:

- Generates the HTML/CSS/JavaScript
- Handles client-server communication
- Manages UI state on the server

**Key benefit**: You build web UIs using the same language as your backend (Java), with full access to your Spring services, databases, and business logic.

## How Vaadin Works

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Vaadin Client Engine (JavaScript)                       │    │
│  │  - Renders UI components                                 │    │
│  │  - Sends user events to server                          │    │
│  │  - Receives UI updates from server                      │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ WebSocket / HTTP
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Server (JVM)                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Your Java Code                                          │    │
│  │  - UI components (Button, TextField, layouts)            │    │
│  │  - Event handlers                                        │    │
│  │  - Business logic                                        │    │
│  │  - Spring services                                       │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

**Flow**:
1. User clicks a button in the browser
2. Click event sent to server via WebSocket
3. Your Java event handler runs on the server
4. You modify UI components (e.g., add text to a message list)
5. Vaadin sends DOM updates back to browser
6. Browser renders the changes

## Project Structure

```
src/main/
├── java/.../ui/
│   ├── view/
│   │   └── MainView.java        # Routes/pages (like React Router)
│   └── component/
│       ├── Chat.java            # Reusable component (like React component)
│       ├── ChatHeader.java
│       ├── ChatMessage.java
│       └── SettingsPanel.java
└── frontend/
    └── themes/spring-ai-vaadin/
        ├── styles.css           # Main theme entry point
        ├── chat.css             # Component-specific styles
        └── ...
```

## Core Concepts

### Components

Everything in Vaadin is a **Component**. Components are Java objects that represent UI elements:

```java
// Create a button
Button button = new Button("Click me");

// Create a text field
TextField name = new TextField("Your name");

// Create a heading
H1 title = new H1("Welcome");
```

### Component Hierarchy

Components form a tree, just like HTML DOM:

```java
// Parent layout contains children
VerticalLayout layout = new VerticalLayout();
layout.add(new H1("Title"));           // Child 1
layout.add(new TextField("Name"));      // Child 2
layout.add(new Button("Submit"));       // Child 3
```

This generates HTML like:
```html
<vaadin-vertical-layout>
  <h1>Title</h1>
  <vaadin-text-field label="Name"></vaadin-text-field>
  <vaadin-button>Submit</vaadin-button>
</vaadin-vertical-layout>
```

### Creating Custom Components

Extend a layout or component class:

```java
// From this app: Chat.java
public class Chat extends VerticalLayout {
    private final MessageList messageList;
    private final MessageInput messageInput;

    public Chat() {
        // Add children
        add(messageList);
        add(messageInput);
    }
}
```

## Components Used in This App

### Layout Components

| Component | Purpose | Used In |
|-----------|---------|---------|
| `VerticalLayout` | Stack children vertically | Chat, SettingsPanel |
| `HorizontalLayout` | Stack children horizontally | ChatHeader |
| `MasterDetailLayout` | Two-panel layout (main + sidebar) | MainView |
| `Scroller` | Scrollable container | Chat (for messages) |

### Input Components

| Component | Purpose | Used In |
|-----------|---------|---------|
| `MessageInput` | Chat input with send button | Chat |
| `TextArea` | Multi-line text input | SettingsPanel (system message) |
| `Checkbox` | Boolean toggle | SettingsPanel (Use MCP) |
| `Upload` | File upload | Chat, SettingsPanel |
| `Button` | Clickable button | ChatHeader, SettingsPanel |

### Display Components

| Component | Purpose | Used In |
|-----------|---------|---------|
| `MessageList` | Chat message display | Chat |
| `H1`, `H3`, `H4` | Headings | ChatHeader, SettingsPanel |
| `Span` | Inline text | ChatHeader |
| `Icon` | Vaadin icons | ChatHeader buttons |
| `UnorderedList`, `ListItem` | Lists | SettingsPanel (files) |

### Example: MessageList and MessageInput

These are Vaadin's built-in chat components:

```java
// Create message list with Markdown support
MessageList messageList = new MessageList();
messageList.setMarkdown(true);  // Enable Markdown rendering

// Add a message
MessageListItem item = new MessageListItem();
item.setText("Hello **world**!");  // Markdown bold
item.setUserName("Assistant");
messageList.addItem(item);

// Create input
MessageInput input = new MessageInput();
input.addSubmitListener(event -> {
    String message = event.getValue();  // Get typed text
    // Handle the message...
});
```

### Example: Upload Component

```java
// Create upload with in-memory handler
Upload upload = new Upload(UploadHandler.inMemory((meta, data) -> {
    // meta.fileName()    - "document.pdf"
    // meta.contentType() - "application/pdf"
    // data               - byte[] of file contents

    processFile(meta.fileName(), data);
}));

// Configure limits
upload.setMaxFiles(10);
upload.setMaxFileSize(10 * 1024 * 1024);  // 10MB
upload.setAcceptedFileTypes(".pdf", ".txt", "image/*");
```

## Layouts

### VerticalLayout

Stacks children top-to-bottom:

```java
VerticalLayout layout = new VerticalLayout();
layout.add(component1, component2, component3);

// Sizing
layout.setSizeFull();        // Fill parent
layout.setWidthFull();       // Full width only
layout.setPadding(true);     // Add padding
layout.setSpacing(true);     // Add space between children

// Flex properties (like CSS flexbox)
layout.setFlexGrow(1, component1);  // component1 grows to fill space
layout.setFlexShrink(0, component2); // component2 doesn't shrink
```

### HorizontalLayout

Stacks children left-to-right:

```java
HorizontalLayout header = new HorizontalLayout();

// Add to specific positions
header.addToStart(logo);     // Left side
header.addToEnd(button1, button2);  // Right side

header.setFlexGrow(1, logo);  // Logo expands, buttons stay fixed
```

### MasterDetailLayout

Two-panel responsive layout used in MainView:

```java
public class MainView extends MasterDetailLayout {
    public MainView() {
        // Main content (always visible)
        setMaster(chatContent);

        // Detail panel (sidebar, can be toggled)
        setDetail(settingsPanel);  // Show panel
        setDetail(null);           // Hide panel

        // Configuration
        setDetailMinSize("400px");
        setDetailSize("600px");
    }
}
```

## Routing

Vaadin uses annotations to define routes (pages):

```java
@Route("")                           // URL path: /
@PageTitle("Spring AI Assistant")    // Browser tab title
@PermitAll                           // Security: allow all authenticated users
public class MainView extends MasterDetailLayout {
    // This view is shown at http://localhost:8080/
}
```

Multiple routes example:
```java
@Route("settings")  // http://localhost:8080/settings
public class SettingsView extends VerticalLayout { }

@Route("chat/:id")  // http://localhost:8080/chat/123
public class ChatView extends VerticalLayout {
    // Access route parameter
    @Override
    public void setParameter(BeforeEvent event, String id) {
        // id = "123"
    }
}
```

## Event Handling

### Click Events

```java
Button button = new Button("Click me");

// Lambda syntax
button.addClickListener(event -> {
    Notification.show("Clicked!");
});

// Method reference
button.addClickListener(this::handleClick);

private void handleClick(ClickEvent<Button> event) {
    // event.getSource() returns the button
}
```

### Custom Events via Listeners

This app uses a pattern for component communication:

```java
// In Chat.java - Define listener interface
public interface ChatSubmitListener {
    void onSubmit(ChatMessage userMessage, ChatMessage assistantMessage);
}

// Store listener
private ChatSubmitListener listener;

public void setSubmitListener(ChatSubmitListener listener) {
    this.listener = listener;
}

// Call listener when event occurs
private void sendMessage(String message) {
    listener.onSubmit(userMessage, assistantMessage);
}

// In MainView.java - Use the listener
chat.setSubmitListener(this::handleSubmit);

private void handleSubmit(ChatMessage user, ChatMessage assistant) {
    // Handle the chat submission
}
```

### Registration Pattern

Listeners return a `Registration` for cleanup:

```java
// In ChatHeader.java
public Registration addNewChatListener(
        ComponentEventListener<ClickEvent<Button>> listener) {
    return newChatButton.addClickListener(listener);
}

// Usage - can remove listener later
Registration reg = header.addNewChatListener(e -> resetChat());
reg.remove();  // Unsubscribe
```

## Server Push (Real-time Updates)

**The Problem**: Vaadin UI updates only happen during request-response cycles. But AI responses stream asynchronously - how do we update the UI?

**The Solution**: Server Push allows the server to push updates to the browser anytime.

### Enable Push

In the main application class:

```java
@Push  // Enable server push
@SpringBootApplication
public class SpringAiVaadinApplication implements AppShellConfigurator {
}
```

### Using UI.access()

When updating UI from a background thread, you **must** use `UI.access()`:

```java
// In MainView.java
private void handleSubmit(ChatMessage userMessage, ChatMessage assistantMessage) {
    var ui = getUI().get();  // Get UI reference while on UI thread

    assistant.stream(chatId, message, attachments, options)
        .subscribe(
            // Called on background thread for each token
            token -> ui.access(() -> {
                // This runs on UI thread - safe to modify components
                assistantMessage.appendText(token);
            }),

            // Error handler
            error -> ui.access(() -> {
                assistantMessage.appendText("Error: " + error.getMessage());
            }),

            // Completion handler
            () -> ui.access(() -> {
                chat.removeClassName("streaming");
            })
        );
}
```

**Why `ui.access()`?**
- Vaadin components are not thread-safe
- `ui.access()` queues the update on the UI thread
- Push sends the update to the browser

### Common Pattern

```java
// 1. Capture UI reference on the request thread
var ui = getUI().get();

// 2. Start async operation
CompletableFuture.runAsync(() -> {
    String result = slowOperation();

    // 3. Update UI from background thread
    ui.access(() -> {
        resultLabel.setText(result);
    });
});
```

## Styling and Themes

### Theme Structure

```
src/main/frontend/themes/spring-ai-vaadin/
├── styles.css          # Main entry point (imports others)
├── chat.css            # Styles for Chat component
├── chat-header.css     # Styles for ChatHeader
├── main-view.css       # Styles for MainView
└── settings-panel.css  # Styles for SettingsPanel
```

### Applying Theme

In application class:

```java
@Theme("spring-ai-vaadin")  // Folder name under themes/
public class SpringAiVaadinApplication { }
```

### Adding CSS Classes

```java
// Add class to component
chat.addClassName("chat-component");
messageList.addClassName("message-list");

// Multiple classes
button.addClassNames("primary", "large");

// Remove class
chat.removeClassName("streaming");

// Conditional class
chat.setClassName("streaming", isStreaming);
```

### CSS Selectors

```css
/* By class name */
.chat-component {
    display: flex;
    flex-direction: column;
}

/* Vaadin component tag names */
vaadin-message-list {
    flex: 1;
    overflow: auto;
}

vaadin-button {
    margin: 8px;
}

/* Theme variants */
vaadin-button[theme~="primary"] {
    background: blue;
}
```

### Button Themes

```java
// Built-in theme variants
button.addThemeNames("icon", "small", "contrast", "tertiary");

// Common variants:
// - "primary"   - Primary action (colored)
// - "tertiary"  - Minimal styling
// - "contrast"  - High contrast
// - "icon"      - Icon-only button
// - "small"     - Smaller size
// - "large"     - Larger size
```

## Spring Integration

### Dependency Injection

Vaadin views are Spring-managed beans - use constructor injection:

```java
@Route("")
public class MainView extends MasterDetailLayout {

    private final Assistant assistant;
    private final RagContextService ragContextService;

    // Spring injects these automatically
    public MainView(Assistant assistant, RagContextService ragContextService) {
        this.assistant = assistant;
        this.ragContextService = ragContextService;
    }
}
```

### View Scope

Each browser tab gets its own view instance:

```java
@Route("")
public class MainView extends MasterDetailLayout {
    // Each user/tab has their own chatId
    private String chatId = UUID.randomUUID().toString();
}
```

## Code Walkthrough

### MainView.java - The Main Page

```java
@Route("")                           // Handles http://localhost:8080/
@PageTitle("Spring AI Assistant")
@PermitAll
public class MainView extends MasterDetailLayout {  // Two-panel layout

    public MainView(Assistant assistant, RagContextService ragContextService) {
        // Create components
        var chatHeader = new ChatHeader();
        this.chat = new Chat();
        this.settingsPanel = new SettingsPanel(ragContextService);

        // Wire up event listeners
        chatHeader.addNewChatListener(e -> resetChat());
        chatHeader.addToggleSettingsListener(e -> toggleSettings());
        chat.setSubmitListener(this::handleSubmit);

        // Build layout: header + chat in vertical stack
        var chatContent = new VerticalLayout(chatHeader, chat);

        // Set as main content
        setMaster(chatContent);
    }

    private void handleSubmit(ChatMessage userMessage, ChatMessage assistantMessage) {
        var ui = getUI().get();  // Capture for background thread

        // Stream AI response
        assistant.stream(chatId, message, attachments, options)
            .subscribe(
                token -> ui.access(() -> assistantMessage.appendText(token))
            );
    }

    private void toggleSettings() {
        // Toggle detail panel visibility
        setDetail(getDetail() == null ? settingsPanel : null);
    }
}
```

### Chat.java - Reusable Chat Component

```java
public class Chat extends VerticalLayout {

    public Chat() {
        // Message display
        messageList = new MessageList();
        messageList.setMarkdown(true);

        // Scrollable container for messages
        var scroller = new Scroller(messageList);
        setFlexGrow(1, scroller);  // Takes remaining space
        add(scroller);

        // File upload
        upload = new Upload(createUploadHandler());

        // Message input
        messageInput = new MessageInput();
        messageInput.addSubmitListener(event -> sendMessage(event.getValue()));

        // Nest input inside upload area
        upload.getElement().appendChild(messageInput.getElement());
        add(upload);
    }

    private void sendMessage(String message) {
        // Create user message and add to list
        var userMessage = new ChatMessage("User", message, pendingAttachments);
        messageList.addItem(userMessage.messageListItem);

        // Create placeholder for assistant response
        var assistantMessage = new ChatMessage("Assistant", null, null);
        messageList.addItem(assistantMessage.messageListItem);

        // Notify listener (MainView)
        chatSubmitListener.onSubmit(userMessage, assistantMessage);
    }
}
```

### ChatHeader.java - Header with Buttons

```java
public class ChatHeader extends HorizontalLayout {

    public ChatHeader() {
        setWidthFull();

        // Title on left
        var heading = new H1("Spring AI Assistant");
        addToStart(heading);

        // Buttons on right
        newChatButton = new Button(new Icon(VaadinIcon.PLUS));
        settingsButton = new Button(new Icon(VaadinIcon.COG));
        addToEnd(newChatButton, settingsButton);
    }

    // Expose click events
    public Registration addNewChatListener(
            ComponentEventListener<ClickEvent<Button>> listener) {
        return newChatButton.addClickListener(listener);
    }
}
```

## Quick Reference

### Common Operations

```java
// Show notification
Notification.show("Message sent!");

// Navigate to another view
UI.getCurrent().navigate("settings");

// Get current UI
UI ui = getUI().get();

// Run on UI thread from background
ui.access(() -> label.setText("Updated"));

// Add/remove CSS class
component.addClassName("active");
component.removeClassName("active");

// Enable/disable
button.setEnabled(false);

// Show/hide
component.setVisible(false);

// Size
layout.setSizeFull();
layout.setWidth("300px");
layout.setHeight("100%");
```

### Component Creation Cheat Sheet

```java
// Text
new Span("text");
new H1("heading");
new Paragraph("paragraph");

// Input
new TextField("Label");
new TextArea("Label");
new Checkbox("Label");
new Button("Label");
new Button(new Icon(VaadinIcon.PLUS));

// Layout
new VerticalLayout(child1, child2);
new HorizontalLayout(child1, child2);
new Scroller(content);

// Chat
new MessageList();
new MessageInput();

// Upload
new Upload(UploadHandler.inMemory((meta, data) -> { }));
```

## Further Reading

- [Vaadin Documentation](https://vaadin.com/docs)
- [Vaadin Component Directory](https://vaadin.com/directory)
- [Vaadin Cookbook](https://cookbook.vaadin.com)
