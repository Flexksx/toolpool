package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.adapter.mcp.McpDirectTools;
import io.github.flexksx.toolpool.adapter.openapi.OpenApiToolCatalogSource;
import io.github.flexksx.toolpool.application.ToolCallExecutor;
import io.github.flexksx.toolpool.application.ToolCatalogSource;
import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.Toolpool;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class ToolpoolAutoConfiguration {

  private static final String MODE_PROPERTY = "toolpool.mode";
  private static final String MODE_METATOOLS = "metatools";
  private static final String MODE_DIRECT = "direct";

  @Bean
  @ConditionalOnMissingBean
  ToolCatalogSource toolCatalogSource(@Value("${toolpool.spec-location}") String specLocation) {
    return new OpenApiToolCatalogSource(specLocation);
  }

  @Bean
  @ConditionalOnMissingBean
  ToolCallExecutor toolCallExecutor(@Value("${toolpool.base-url}") String baseUrl) {
    return new RestClientToolCallExecutor(RestClient.builder().baseUrl(baseUrl).build());
  }

  @Bean
  @ConditionalOnMissingBean
  Toolpool toolpool(ToolCatalogSource catalogSource, ToolCallExecutor callExecutor) {
    return new Toolpool(catalogSource, callExecutor);
  }

  @Bean
  @ConditionalOnProperty(name = MODE_PROPERTY, havingValue = MODE_METATOOLS, matchIfMissing = true)
  McpGatewayMetatools mcpGatewayMetatools(Toolpool toolpool) {
    return new McpGatewayMetatools(toolpool);
  }

  @Bean
  @ConditionalOnProperty(name = MODE_PROPERTY, havingValue = MODE_DIRECT)
  List<SyncToolSpecification> directToolSpecifications(Toolpool toolpool)
      throws ToolCatalogUnavailableException {
    return new McpDirectTools(toolpool).toolSpecifications();
  }
}
