# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build and run tests
./mvnw install

# Run the application (available at http://localhost:8081)
./mvnw spring-boot:run

# Run a single test
./mvnw test -Dtest=SpringAiVaadinApplicationTests

# Production build (includes Vaadin frontend compilation)
./mvnw install -Pproduction
```

## Required Environment Variables

```bash
export OPENAI_API_KEY=your-openai-key
export KEYCLOAK_CLIENT_SECRET=your-keycloak-secret  # For OAuth2 security
```

## Architecture Overview

This is a Vaadin + Spring AI chat application with OAuth2/Keycloak security and MCP (Model Context Protocol) integration for tool calling.

### Key Components

**Entry Point & Configuration:**
- `SpringAiVaadinApplication` - Main class with `@Push` for real-time streaming, excludes Vaadin's default security
- `McpSecurityConfig` - OAuth2/Keycloak security, MCP client security beans, Reactor context propagation
- `AiConfig` - Vector store configuration for RAG

**Service Layer:**
- `Assistant` - Core AI service that configures ChatClient with advisors (memory, RAG, safeguard) and handles streaming responses with MCP tool callbacks

**UI Layer (Vaadin):**
- `MainView` - Master-detail layout with chat and settings panel
- `Chat` - Chat component with message list, input, and file upload
- `SettingsPanel` - System message configuration, MCP toggle, RAG document upload

### Security Context Propagation

Spring AI's chat client uses reactive streams on `boundedElastic` threads. For OAuth2 tokens to reach MCP servers:

1. `Assistant.stream()` uses `.contextWrite(AuthenticationMcpTransportContextProvider.writeToReactorContext())` to propagate SecurityContext to Reactor context
2. `McpSecurityConfig` defines `AuthorizedClientServiceOAuth2AuthorizedClientManager` (not the default manager) because MCP tool calls execute after the original servlet request is recycled
3. `OAuth2AuthorizationCodeSyncHttpRequestCustomizer` adds Bearer tokens to MCP HTTP requests

### MCP Integration

- Uses Streamable HTTP transport to connect to MCP servers (configured in application.properties)
- MCP tools are enabled via settings panel toggle
- Tokens propagate to MCP server allowing `@PreAuthorize` checks on server-side tools

## Important Patterns

- Vaadin Push (`@Push`) enables streaming token-by-token responses via `ui.access()`
- ChatClient advisors chain: SafeGuard → Memory → RAG pipeline
- File attachments: images become Media objects, text/PDF parsed via Tika into context

## See Also

- `SECURITY.md` - Detailed OAuth2/Keycloak setup and troubleshooting
