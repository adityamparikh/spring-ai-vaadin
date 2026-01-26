package org.spring.framework.ai.vaadin;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the Spring AI Vaadin Assistant.
 *
 * <p>This application provides a chat interface for interacting with AI models using Spring AI,
 * with a Vaadin-based UI that supports real-time streaming responses, file attachments,
 * and Retrieval-Augmented Generation (RAG).
 *
 * <p>Key features:
 * <ul>
 *   <li>Server push enabled for real-time AI response streaming</li>
 *   <li>Custom theme "spring-ai-vaadin" for styling</li>
 *   <li>Vaadin's built-in security is excluded in favor of custom OAuth2/Keycloak configuration</li>
 * </ul>
 *
 * @see McpSecurityConfig for security configuration
 * @see org.spring.framework.ai.vaadin.ui.view.MainView for the main UI
 */
@Push
@SpringBootApplication(exclude = {SpringSecurityAutoConfiguration.class})
@Theme("spring-ai-vaadin")
public class SpringAiVaadinApplication implements AppShellConfigurator {

  /**
   * Application entry point.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(SpringAiVaadinApplication.class, args);
  }
}
