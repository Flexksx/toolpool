package io.github.flexksx.toolpool.spring.server;

import io.github.flexksx.toolpool.spring.mcp.McpServerDefaultProperties;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Turns the MCP server off in the main application when Toolpool has its own port, so that only the
 * Toolpool server answers MCP requests.
 */
public class ToolpoolServerEnvironment implements EnvironmentPostProcessor {

  static final String PROPERTY_SOURCE_NAME = "toolpoolServerDefaults";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (environment.containsProperty(ToolpoolServerAutoConfiguration.PORT_PROPERTY)) {
      environment
          .getPropertySources()
          .addLast(
              new MapPropertySource(
                  PROPERTY_SOURCE_NAME,
                  Map.of(McpServerDefaultProperties.ENABLED_PROPERTY, Boolean.FALSE.toString())));
    }
  }
}
