package org.spring.framework.ai.vaadin;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Push
@SpringBootApplication(exclude = {SpringSecurityAutoConfiguration.class})
@Theme("spring-ai-vaadin")
public class SpringAiVaadinApplication implements AppShellConfigurator {

  public static void main(String[] args) {
    SpringApplication.run(SpringAiVaadinApplication.class, args);
  }
}
