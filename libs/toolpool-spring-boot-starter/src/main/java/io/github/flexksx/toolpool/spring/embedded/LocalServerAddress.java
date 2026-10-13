package io.github.flexksx.toolpool.spring.embedded;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

final class LocalServerAddress implements ApplicationListener<WebServerInitializedEvent> {

  private static final String LOCAL_HOST = "http://localhost:";

  private final String contextPath;

  private volatile @Nullable String baseUrl;

  LocalServerAddress(String contextPath) {
    this.contextPath = contextPath;
  }

  @Override
  public void onApplicationEvent(WebServerInitializedEvent event) {
    if (event.getApplicationContext().getServerNamespace() == null) {
      baseUrl = LOCAL_HOST + event.getWebServer().getPort() + contextPath;
    }
  }

  String baseUrl() {
    String current = baseUrl;
    if (current == null) {
      throw new IllegalStateException(
          "The web server has not started yet, so Toolpool does not know its local address");
    }
    return current;
  }
}
