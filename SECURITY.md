# OAuth2/Keycloak Security Setup for MCP Client

This document explains how to configure OAuth2 authentication with Keycloak for the Spring AI MCP (Model Context Protocol) client in this Vaadin application.

## Overview

This application uses **OAuth2 Authorization Code Flow** to authenticate users via Keycloak. The authenticated user's credentials are then propagated to MCP server requests, allowing the MCP server to enforce user-based access control.

### Architecture

```
┌─────────────┐     ┌───────────────┐     ┌─────────────────┐     ┌────────────┐
│   Browser   │────>│ Vaadin App    │────>│  Keycloak       │────>│ MCP Server │
│             │<────│ (port 8081)   │<────│  (port 8180)    │<────│ (port 8080)│
└─────────────┘     └───────────────┘     └─────────────────┘     └────────────┘
                           │                      │
                           │  OAuth2 Access Token │
                           └──────────────────────┘
```

## Prerequisites

1. **Keycloak Server** running on `http://localhost:8180`
2. **MCP Server** running on `http://localhost:8080` (e.g., Apache Solr MCP Server)
3. **Java 21** or later

## Keycloak Configuration

### 1. Create a Realm

Create a realm named `solr-mcp` (or your preferred name).

### 2. Create a Client

Create a client with the following settings:

| Setting | Value |
|---------|-------|
| Client ID | `solr-mcp-client` |
| Client Protocol | `openid-connect` |
| Access Type | `confidential` |
| Client authentication | `ON` (enabled) |
| Valid Redirect URIs | `http://localhost:8081/*` |
| Web Origins | `http://localhost:8081` |

### 3. Get the Client Secret

1. Go to your client's **Credentials** tab
2. Copy the **Client secret** value
3. Set it as an environment variable: `KEYCLOAK_CLIENT_SECRET`

### 4. Create Users

Create users in your Keycloak realm that will be able to log in to the application.

## Application Configuration

### application.properties

```properties
# MCP Client Configuration
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.name=vaadin-mcp-client
spring.ai.mcp.client.version=1.0.0
spring.ai.mcp.client.type=SYNC
spring.ai.mcp.client.request-timeout=30s
spring.ai.mcp.client.streamable-http.connections.solr.url=http://localhost:8080
spring.ai.mcp.client.streamable-http.connections.solr.endpoint=/mcp

# Keycloak OAuth2 Provider
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:8180/realms/solr-mcp

# Keycloak Client Registration (Authorization Code Flow)
spring.security.oauth2.client.registration.keycloak.client-id=solr-mcp-client
spring.security.oauth2.client.registration.keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET}
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.scope=openid,profile
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
```

### Environment Variables

Set the following environment variable before running the application:

```bash
export KEYCLOAK_CLIENT_SECRET=your-client-secret-here
```

Or configure it in your IDE's run configuration.

## Security Components

### McpSecurityConfig.java

This is the core security configuration class that:

1. **Configures Spring Security Filter Chain** - Sets up authorization rules, permits static resources, and requires authentication for all other requests.

2. **Configures OAuth2 Login** - Uses Keycloak as the identity provider with `VaadinSavedRequestAwareAuthenticationSuccessHandler` to properly handle Vaadin navigation after login.

3. **Enables Reactor Context Propagation** - Ensures `SecurityContext` is propagated to reactive threads (see below).

4. **Configures MCP Client Security** - Two beans work together to propagate OAuth2 tokens to MCP requests:
   - `McpSyncClientCustomizer` - Adds authentication context to MCP transport
   - `McpSyncHttpClientRequestCustomizer` - Captures and caches OAuth2 access token, adds Authorization header to MCP HTTP requests

### Reactor Context Propagation

**The Problem:** Spring AI's chat client uses reactive streams internally, which execute on Reactor's `boundedElastic` threads. The `SecurityContext` stored in ThreadLocal is not automatically available on these reactive threads, causing MCP tool calls to fail with "Access Denied" even when the user is authenticated.

**The Solution:** Use `contextWrite()` to propagate the `SecurityContext` to the Reactor context when streaming:

```java
// In your service that calls ChatClient.stream()
return chatClient.prompt()
    // ... configure prompt ...
    .stream().content()
    .contextWrite(AuthenticationMcpTransportContextProvider.writeToReactorContext());
```

This captures the `SecurityContext` at the start of the stream (on the servlet thread where it's available) and writes it to the Reactor context, making it available downstream when MCP tool calls execute.

### Key Beans

```java
@Bean
McpSyncClientCustomizer mcpSyncClientCustomizer() {
    return (name, syncSpec) -> syncSpec
        .transportContextProvider(new AuthenticationMcpTransportContextProvider());
}

/**
 * OAuth2AuthorizedClientManager that works outside of HTTP request context.
 * Required because MCP tool calls execute on Reactor threads after the original
 * servlet request has been recycled by Tomcat.
 */
@Bean
OAuth2AuthorizedClientManager authorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientService authorizedClientService) {
    return new AuthorizedClientServiceOAuth2AuthorizedClientManager(
        clientRegistrationRepository, authorizedClientService);
}

@Bean
McpSyncHttpClientRequestCustomizer mcpAuthorizationCodeCustomizer(
        OAuth2AuthorizedClientManager authorizedClientManager) {
    return new OAuth2AuthorizationCodeSyncHttpRequestCustomizer(
        authorizedClientManager, "keycloak");
}
```

**Important:** The `AuthorizedClientServiceOAuth2AuthorizedClientManager` is required instead of the default `DefaultOAuth2AuthorizedClientManager`. The default manager tries to access `HttpServletRequest` to get request parameters, but by the time MCP tool calls execute on Reactor threads, the original servlet request has been recycled by Tomcat, causing `IllegalStateException: The request object has been recycled`.

The `AuthenticationMcpTransportContextProvider` reads the authentication from the Reactor context (written via `contextWrite()`), and the `OAuth2AuthorizationCodeSyncHttpRequestCustomizer` uses the service-based manager to add the Bearer token to MCP requests.

## Dependencies

The following dependencies are required in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>mcp-client-security</artifactId>
    <version>0.1.0</version>
</dependency>
<!-- Required for Reactor context propagation (SecurityContext to boundedElastic threads) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>context-propagation</artifactId>
</dependency>
```

## Authentication Flow

1. **User Accesses Application** - User navigates to `http://localhost:8081`

2. **Redirect to Keycloak** - Spring Security redirects unauthenticated users to Keycloak login page (`/oauth2/authorization/keycloak`)

3. **User Authenticates** - User enters credentials in Keycloak

4. **Callback with Auth Code** - Keycloak redirects back to `/login/oauth2/code/keycloak` with an authorization code

5. **Token Exchange** - Spring Security exchanges the auth code for access and refresh tokens

6. **Session Established** - User is now authenticated and can access the application

7. **MCP Requests** - When the application makes MCP requests, the OAuth2 access token is automatically included in the `Authorization` header

## Using MCP Tools

The application includes a settings panel with a toggle to enable/disable MCP tools. When enabled:

1. The chat assistant will have access to tools provided by the MCP server
2. All MCP requests include the user's OAuth2 access token
3. The MCP server can enforce access control based on the authenticated user

## Troubleshooting

### "Connection lost" Error

This typically indicates Vaadin's WebSocket connections are being blocked. Ensure these paths are permitted in security configuration:
- `/VAADIN/**`
- `/vaadinServlet/**`
- `/PUSH/**`

### Login Not Appearing

1. Verify Keycloak is running and accessible
2. Check that `anonymous` access is disabled
3. Ensure `SpringSecurityAutoConfiguration` is excluded from the main application class

### Invalid Client Credentials

1. Verify **Client authentication** is enabled in Keycloak client settings
2. Check the client secret matches the `KEYCLOAK_CLIENT_SECRET` environment variable
3. Verify the issuer URI matches your Keycloak realm URL

### MCP Server Not Discovered

Ensure MCP client properties are correctly configured:
- `spring.ai.mcp.client.enabled=true`
- Check the URL and endpoint are correct
- Verify the MCP server is running and accessible

### MCP Tool Calls Return "Access Denied"

If MCP tool calls fail with "Access Denied" even though the user is authenticated:

1. **Check thread names in logs** - If tool calls run on `boundedElastic-*` threads (Reactor threads), the SecurityContext may not be propagating.

2. **Verify `contextWrite()` is used** - Ensure your ChatClient stream includes:
   ```java
   .contextWrite(AuthenticationMcpTransportContextProvider.writeToReactorContext())
   ```

3. **Enable debug logging** to trace authentication:
   ```properties
   logging.level.org.springframework.security=DEBUG
   ```

4. **Check MCP server logs** - Look for `Set SecurityContextHolder to anonymous` which indicates the token isn't being received.

### "The request object has been recycled" Error

If you see `IllegalStateException: The request object has been recycled and is no longer associated with this facade`:

**Cause:** The default `DefaultOAuth2AuthorizedClientManager` tries to access the HTTP servlet request, but MCP tool calls execute on Reactor threads after the original request has been recycled.

**Solution:** Define a custom `OAuth2AuthorizedClientManager` bean using `AuthorizedClientServiceOAuth2AuthorizedClientManager`:

```java
@Bean
OAuth2AuthorizedClientManager authorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientService authorizedClientService) {
    return new AuthorizedClientServiceOAuth2AuthorizedClientManager(
        clientRegistrationRepository, authorizedClientService);
}
```

This manager stores authorized clients in `OAuth2AuthorizedClientService` and doesn't require an active HTTP request.

## Debug Logging

Enable debug logging to troubleshoot issues:

```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.ai.mcp=DEBUG
logging.level.io.modelcontextprotocol=DEBUG
```

## Version Compatibility

| Component | Version |
|-----------|---------|
| Spring Boot | 4.0.2 |
| Spring AI | 2.0.0-M2 |
| Vaadin | 25.0.3 |
| Java | 21 |
| mcp-client-security | 0.1.0 |
