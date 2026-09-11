package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.adapter.mcp.McpTranslationMode;
import io.github.flexksx.toolpool.adapter.openapi.OpenApiToolCatalogSource;
import io.github.flexksx.toolpool.application.ToolCallExecutor;
import io.github.flexksx.toolpool.application.ToolCatalogSource;
import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.Toolpool;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class ToolpoolAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ToolpoolAutoConfiguration.class);
  private static final String OBSERVED_CLIENT = "context-managed";
  private static final String UNMANAGED_CLIENT = "standalone";

  @Bean
  @ConditionalOnMissingBean
  ToolCatalogSource toolCatalogSource(@Value("${toolpool.spec-location}") String specLocation) {
    LOGGER
        .atInfo()
        .setMessage("Toolpool reads its OpenAPI spec from {}")
        .addArgument(specLocation)
        .log();
    return new OpenApiToolCatalogSource(specLocation);
  }

  @Bean
  @ConditionalOnMissingBean
  ToolCallExecutor toolCallExecutor(
      ObjectProvider<RestClient.Builder> restClientBuilders,
      @Value("${toolpool.base-url}") String baseUrl) {
    RestClient.Builder restClientBuilder = restClientBuilders.getIfAvailable(RestClient::builder);
    LOGGER
        .atInfo()
        .setMessage("Toolpool sends every tool call to {} through a {} RestClient")
        .addArgument(baseUrl)
        .addArgument(
            restClientBuilders.getIfAvailable() == null ? UNMANAGED_CLIENT : OBSERVED_CLIENT)
        .log();
    return new RestClientToolCallExecutor(restClientBuilder.baseUrl(baseUrl).build());
  }

  @Bean
  @ConditionalOnMissingBean
  Toolpool toolpool(ToolCatalogSource catalogSource, ToolCallExecutor callExecutor) {
    return new Toolpool(catalogSource, callExecutor);
  }

  @Bean
  List<SyncToolSpecification> toolpoolToolSpecifications(
      Toolpool toolpool, @Value("${toolpool.mode:METATOOLS}") McpTranslationMode mode)
      throws ToolCatalogUnavailableException {
    List<SyncToolSpecification> specifications = mode.translationOf(toolpool).toolSpecifications();
    LOGGER
        .atInfo()
        .setMessage("Toolpool runs in {} mode and exposes {} MCP tools")
        .addArgument(mode)
        .addArgument(specifications.size())
        .log();
    return specifications;
  }
}
