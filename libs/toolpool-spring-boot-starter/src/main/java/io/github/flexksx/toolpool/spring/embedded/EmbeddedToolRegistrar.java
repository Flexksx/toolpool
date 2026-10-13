package io.github.flexksx.toolpool.spring.embedded;

import io.github.flexksx.toolpool.adapter.mcp.McpToolUseCases;
import io.github.flexksx.toolpool.adapter.mcp.McpTranslationMode;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

final class EmbeddedToolRegistrar implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedToolRegistrar.class);

  private final McpToolUseCases useCases;
  private final McpSyncServer server;
  private final McpTranslationMode mode;

  EmbeddedToolRegistrar(McpToolUseCases useCases, McpSyncServer server, McpTranslationMode mode) {
    this.useCases = useCases;
    this.server = server;
    this.mode = mode;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    List<SyncToolSpecification> specifications;
    try {
      specifications = mode.translationOf(useCases).toolSpecifications();
    } catch (ToolCatalogUnavailableException failure) {
      throw new IllegalStateException(
          "Toolpool runs embedded and reads the local OpenAPI spec that springdoc serves."
              + " Check that springdoc is on the classpath and that its api-docs path is open",
          failure);
    }
    specifications.forEach(server::addTool);
    LOGGER
        .atInfo()
        .setMessage("Toolpool runs embedded in {} mode and exposes {} MCP tools")
        .addArgument(mode)
        .addArgument(specifications.size())
        .log();
  }
}
