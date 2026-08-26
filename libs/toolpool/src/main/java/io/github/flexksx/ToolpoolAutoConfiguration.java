package io.github.flexksx;

import io.github.flexksx.adapter.http.RestClientToolCallExecutor;
import io.github.flexksx.adapter.mcp.McpDirectTools;
import io.github.flexksx.adapter.mcp.McpGatewayMetatools;
import io.github.flexksx.adapter.openapi.OpenApiToolCatalogProvider;
import io.github.flexksx.application.ToolCallExecutor;
import io.github.flexksx.application.ToolCatalogProvider;
import io.github.flexksx.application.ToolCatalogUnavailableException;
import io.github.flexksx.application.Toolpool;
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
  ToolCatalogProvider toolCatalogProvider(@Value("${toolpool.spec-location}") String specLocation) {
    return new OpenApiToolCatalogProvider(specLocation);
  }

  @Bean
  @ConditionalOnMissingBean
  ToolCallExecutor toolCallExecutor(@Value("${toolpool.base-url}") String baseUrl) {
    return new RestClientToolCallExecutor(RestClient.builder().baseUrl(baseUrl).build());
  }

  @Bean
  @ConditionalOnMissingBean
  Toolpool toolpool(ToolCatalogProvider catalogProvider, ToolCallExecutor callExecutor) {
    return new Toolpool(catalogProvider, callExecutor);
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
