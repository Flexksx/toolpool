package io.github.flexksx.toolpool.spring.server;

import io.github.flexksx.toolpool.spring.mcp.McpServerDefaultProperties;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.MapPropertySource;

/**
 * Starts the Toolpool server as a child of the main application once the main application is ready,
 * and closes it before the main application stops.
 */
final class ToolpoolServerLauncher {

  static final String SERVER_NAMESPACE = "toolpool";
  static final String PROPERTY_SOURCE_NAME = "toolpoolServer";
  private static final Logger LOGGER = LoggerFactory.getLogger(ToolpoolServerLauncher.class);

  private final ConfigurableApplicationContext application;
  private final int port;

  private @Nullable ConfigurableApplicationContext server;

  ToolpoolServerLauncher(ConfigurableApplicationContext application, int port) {
    this.application = application;
    this.port = port;
  }

  @EventListener
  public synchronized void start(ApplicationReadyEvent event) {
    if (event.getApplicationContext() != application || server != null) {
      return;
    }
    AnnotationConfigServletWebServerApplicationContext child =
        new AnnotationConfigServletWebServerApplicationContext();
    child.setParent(application);
    child.setServerNamespace(SERVER_NAMESPACE);
    child.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
    child
        .getEnvironment()
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                PROPERTY_SOURCE_NAME,
                Map.of(McpServerDefaultProperties.ENABLED_PROPERTY, Boolean.TRUE.toString())));
    child.registerBean(ServletWebServerFactory.class, this::webServerFactory);
    child.register(ToolpoolServerConfiguration.class);
    child.refresh();
    server = child;
    LOGGER
        .atInfo()
        .setMessage("Toolpool serves MCP on its own port {}")
        .addArgument(child.getWebServer().getPort())
        .log();
  }

  @EventListener
  public synchronized void stop(ContextClosedEvent event) {
    ConfigurableApplicationContext current = server;
    if (event.getApplicationContext() == application && current != null) {
      server = null;
      current.close();
    }
  }

  private ServletWebServerFactory webServerFactory() {
    Class<? extends ServletWebServerFactory> applicationFactoryType =
        application.getBean(ServletWebServerFactory.class).getClass();
    if (!(BeanUtils.instantiateClass(applicationFactoryType)
        instanceof ConfigurableServletWebServerFactory factory)) {
      throw new IllegalStateException(
          applicationFactoryType.getName()
              + " cannot take a port, so Toolpool cannot start its own server on "
              + ToolpoolServerAutoConfiguration.PORT_PROPERTY);
    }
    factory.setPort(port);
    return factory;
  }
}
