package org.spring.framework.ai.vaadin;

import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Hooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.security.client.sync.AuthenticationMcpTransportContextProvider;
import org.springaicommunity.mcp.security.client.sync.oauth2.http.client.OAuth2AuthorizationCodeSyncHttpRequestCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for OAuth2/Keycloak authentication and MCP (Model Context Protocol) integration.
 *
 * <p>This configuration class sets up:
 * <ul>
 *   <li>OAuth2 login flow with Keycloak as the identity provider</li>
 *   <li>Security filter chain with appropriate access rules for Vaadin resources</li>
 *   <li>MCP client customization for authenticated tool calls</li>
 *   <li>Reactor context propagation for SecurityContext across async boundaries</li>
 * </ul>
 *
 * <p>The configuration ensures that MCP tool calls executed on Reactor threads
 * can access the authenticated user's OAuth2 tokens for secure API interactions.
 *
 * @see AuthenticationMcpTransportContextProvider for context propagation details
 */
@Configuration
@EnableWebSecurity
public class McpSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(McpSecurityConfig.class);

    /**
     * Initializes Reactor context propagation on application startup.
     *
     * <p>Enables automatic context propagation so that Spring Security's
     * {@code SecurityContext} is available on Reactor's boundedElastic threads,
     * which is required for MCP tool calls that need authentication.
     */
    @PostConstruct
    public void init() {
        // Enable automatic context propagation for Reactor threads
        // This propagates SecurityContext to boundedElastic threads
        Hooks.enableAutomaticContextPropagation();
        log.info(">>> McpSecurityConfig initialized - OAuth2 security and Reactor context propagation enabled");
    }

    /**
     * Configures the main security filter chain for the application.
     *
     * <p>Sets up:
     * <ul>
     *   <li>Public access to Vaadin static resources and frontend assets</li>
     *   <li>Public access to OAuth2 endpoints for login flow</li>
     *   <li>Authentication required for all other requests</li>
     *   <li>OAuth2 login with Keycloak as the provider</li>
     *   <li>CSRF protection disabled for Vaadin (Vaadin handles its own CSRF)</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if configuration fails
     */
    @Bean
    @Primary
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info(">>> Creating security filter chain with OAuth2 login");

        // Authorization rules
        http.authorizeHttpRequests(auth -> {
            auth
                // Static resources
                .requestMatchers("/VAADIN/**").permitAll()
                .requestMatchers("/vaadinServlet/**").permitAll()
                .requestMatchers("/vaadinServlet/PUSH/**").permitAll()
                .requestMatchers("/PUSH/**").permitAll()
                .requestMatchers("/frontend/**").permitAll()
                .requestMatchers("/icons/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/line-awesome/**").permitAll()
                .requestMatchers("/sw.js").permitAll()
                .requestMatchers("/sw-runtime-resources-precache.js").permitAll()
                // OAuth2 endpoints
                .requestMatchers("/login/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated();
            log.info(">>> Authorization rules configured - anyRequest().authenticated()");
        });

        // OAuth2 login
        VaadinSavedRequestAwareAuthenticationSuccessHandler successHandler =
            new VaadinSavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/");

        http.oauth2Login(oauth2 -> {
            oauth2
                .loginPage("/oauth2/authorization/keycloak")
                .successHandler(successHandler);
            log.info(">>> OAuth2 login configured with Keycloak");
        });

        // OAuth2 client for MCP
        http.oauth2Client(oauth2 -> {});

        // CSRF - Vaadin handles its own CSRF
        http.csrf(csrf -> csrf
            .ignoringRequestMatchers("/VAADIN/**", "/vaadinServlet/**", "/**")
        );

        // Disable anonymous access
        http.anonymous(anonymous -> anonymous.disable());

        log.info(">>> Security filter chain built successfully");
        return http.build();
    }

    /**
     * Customizes MCP sync clients to include authentication context.
     *
     * <p>Configures the transport context provider to propagate authentication
     * information to MCP tool calls.
     *
     * @return the MCP client customizer
     */
    @Bean
    McpSyncClientCustomizer mcpSyncClientCustomizer() {
        return (name, syncSpec) -> syncSpec
            .transportContextProvider(new AuthenticationMcpTransportContextProvider());
    }

    /**
     * OAuth2AuthorizedClientManager that works outside of HTTP request context.
     * Required because MCP tool calls execute on Reactor threads after the original
     * servlet request has been recycled.
     */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService);
    }

    /**
     * Creates an HTTP request customizer that adds OAuth2 authorization headers to MCP requests.
     *
     * <p>This customizer intercepts outgoing MCP HTTP requests and adds the appropriate
     * OAuth2 bearer token from the current user's Keycloak session.
     *
     * @param authorizedClientManager the OAuth2 client manager for token retrieval
     * @return the HTTP request customizer
     */
    @Bean
    McpSyncHttpClientRequestCustomizer mcpAuthorizationCodeCustomizer(
            OAuth2AuthorizedClientManager authorizedClientManager) {
        return new OAuth2AuthorizationCodeSyncHttpRequestCustomizer(
            authorizedClientManager, "keycloak");
    }
}
