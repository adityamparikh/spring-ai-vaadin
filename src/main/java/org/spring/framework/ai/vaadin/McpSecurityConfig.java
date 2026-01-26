package org.spring.framework.ai.vaadin;

import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.security.client.sync.AuthenticationMcpTransportContextProvider;
import org.springaicommunity.mcp.security.client.sync.oauth2.http.client.OAuth2AuthorizationCodeSyncHttpRequestCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class McpSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(McpSecurityConfig.class);

    @PostConstruct
    public void init() {
        log.info(">>> McpSecurityConfig initialized - OAuth2 security should be active");
    }

    @Bean
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

    @Bean
    McpSyncClientCustomizer mcpSyncClientCustomizer() {
        return (name, syncSpec) -> syncSpec
            .transportContextProvider(new AuthenticationMcpTransportContextProvider());
    }

    @Bean
    McpSyncHttpClientRequestCustomizer mcpAuthorizationCodeCustomizer(
            OAuth2AuthorizedClientManager authorizedClientManager) {
        return new OAuth2AuthorizationCodeSyncHttpRequestCustomizer(
            authorizedClientManager, "keycloak");
    }
}
