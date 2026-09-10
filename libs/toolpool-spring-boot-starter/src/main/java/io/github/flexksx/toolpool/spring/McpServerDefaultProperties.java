package io.github.flexksx.toolpool.spring;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class McpServerDefaultProperties implements EnvironmentPostProcessor {

  static final String PROPERTY_SOURCE_NAME = "toolpoolMcpServerDefaults";
  static final String PROTOCOL_PROPERTY = "spring.ai.mcp.server.protocol";
  static final String STREAMABLE_PROTOCOL = "STREAMABLE";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    environment
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                PROPERTY_SOURCE_NAME, Map.of(PROTOCOL_PROPERTY, STREAMABLE_PROTOCOL)));
  }
}
