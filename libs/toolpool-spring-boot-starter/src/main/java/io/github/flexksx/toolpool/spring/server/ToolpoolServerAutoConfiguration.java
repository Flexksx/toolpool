package io.github.flexksx.toolpool.spring.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = ToolpoolServerAutoConfiguration.PORT_PROPERTY)
public class ToolpoolServerAutoConfiguration {

  public static final String PORT_PROPERTY = "toolpool.server.port";

  @Bean
  ToolpoolServerLauncher toolpoolServerLauncher(
      ConfigurableApplicationContext application, @Value("${" + PORT_PROPERTY + "}") int port) {
    return new ToolpoolServerLauncher(application, port);
  }
}
