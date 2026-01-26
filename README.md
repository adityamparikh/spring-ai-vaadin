# Spring AI Vaadin

A Vaadin-based chat application for Spring AI, enabling interactive AI-powered chat experiences with support for streaming responses, file attachments, RAG (Retrieval-Augmented Generation), and MCP (Model Context Protocol) tool integration.

## Overview

This project demonstrates how to build AI-powered chat interfaces in a Spring Boot application by combining Spring AI with Vaadin's rich UI components. It features real-time streaming of AI responses, document-based context enhancement through RAG, and optional OAuth2/Keycloak authentication for secure MCP tool usage.

> **Note:** A React-based frontend (using Vaadin [Hilla](https://vaadin.com/docs/latest/hilla/faq)) is available on the `hilla` branch. When switching between branches, delete the `src/main/frontend/generated` directory to avoid startup issues.

## Features

- **Streaming Chat Interface**: Real-time AI responses streamed token-by-token using Vaadin's server push
- **File Attachments**: Upload images, PDFs, and text files as context for your queries
- **RAG Support**: Upload documents to a vector store for context-aware AI responses
- **MCP Integration**: Optional Model Context Protocol support for tool-based capabilities
- **OAuth2/Keycloak Security**: Secure authentication with token propagation to MCP servers
- **Chat Memory**: Conversation history maintained across messages
- **Content Safety**: Built-in SafeGuard advisor for content filtering

## Project Structure

```
src/main/java/org/spring/framework/ai/vaadin/
├── SpringAiVaadinApplication.java   # Application entry point with push & theme config
├── AiConfig.java                    # Spring AI beans (vector store)
├── McpSecurityConfig.java           # OAuth2/Keycloak security configuration
└── service/
│   ├── Assistant.java               # Core AI chat service with RAG pipeline
│   ├── AttachmentFile.java          # File attachment record
│   └── RagContextService.java       # Vector store management for RAG
└── ui/
    ├── view/
    │   └── MainView.java            # Main chat view with master-detail layout
    ├── component/
    │   ├── Chat.java                # Chat component with message list & input
    │   ├── ChatHeader.java          # Header with new chat & settings buttons
    │   ├── ChatMessage.java         # Message display with attachments
    │   └── SettingsPanel.java       # Settings for system message, MCP, RAG
    └── util/
        ├── ImageUtils.java          # Image thumbnail generation
        └── CustomMultipartFile.java # MultipartFile adapter for uploads
```

## Prerequisites

- Java 21 or higher
- OpenAI API key (or other supported LLM provider)
- (Optional) Keycloak server for OAuth2 authentication with MCP

## Getting Started

### 1. Configure your API key

Add your OpenAI API key to your environment variables:

```bash
export OPENAI_API_KEY=your-api-key
```

### 2. Build and run the application

```bash
./mvnw spring-boot:run
```

The application will be available at http://localhost:8080.

## Using the Application

### Chat Interface

Simply type your message and press Enter or click the send button. The AI assistant is configured as a Java and Spring expert by default.

### File Attachments

Click the upload area above the message input to attach files:
- **Images**: Displayed as thumbnails and sent to multimodal AI models
- **PDFs/Text**: Content extracted and included in the prompt

### RAG (Retrieval Augmented Generation)

1. Open the settings panel by clicking the gear icon
2. Upload documents (PDF, DOCX, TXT, MD) to add to the RAG context
3. Ask questions related to the uploaded content

The AI uses a RAG pipeline that:
- Rewrites queries for better search results
- Retrieves relevant documents from the vector store (similarity threshold: 0.50)
- Augments prompts with retrieved context

### MCP Tools

1. Open the settings panel
2. Enable "Use MCP" checkbox
3. The assistant will now have access to tools provided by configured MCP servers

For authenticated MCP servers, see [SECURITY.md](SECURITY.md) for OAuth2/Keycloak setup.

## Configuration

### Basic Configuration (application.properties)

```properties
# OpenAI (default)
spring.ai.openai.api-key=${OPENAI_API_KEY}

# Or use other providers like Anthropic, Ollama, etc.
```

### MCP Configuration

```properties
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.name=vaadin-mcp-client
spring.ai.mcp.client.type=SYNC
spring.ai.mcp.client.streamable-http.connections.myserver.url=http://localhost:8080
spring.ai.mcp.client.streamable-http.connections.myserver.endpoint=/mcp
```

### OAuth2/Keycloak Configuration

For secure MCP integration with user authentication, see [SECURITY.md](SECURITY.md) for detailed setup instructions.

## Technologies

| Component | Version |
|-----------|---------|
| Spring Boot | 4.0.2 |
| Spring AI | 2.0.0-M2 |
| Vaadin | 25.0.3 |
| Java | 21 |
| MCP Client Security | 0.1.0 |

### Key Dependencies

- **Spring Boot**: Application framework
- **Spring AI**: AI capabilities with ChatClient, RAG advisors, and MCP support
- **Vaadin Flow**: Server-side Java UI framework with push support
- **Apache Tika**: Document content extraction for RAG
- **SimpleVectorStore**: In-memory vector storage for embeddings

## API Documentation

All classes are documented with Javadoc. Key classes:

- `Assistant`: Core service handling chat interactions, RAG, and MCP tools
- `RagContextService`: Manages document ingestion into the vector store
- `Chat`: Vaadin component for the chat interface
- `McpSecurityConfig`: OAuth2 security configuration for MCP

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
