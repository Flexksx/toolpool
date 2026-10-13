package io.github.flexksx.toolpool.spring.embedded;

import io.github.flexksx.toolpool.adapter.mcp.McpToolUseCases;
import io.github.flexksx.toolpool.adapter.mcp.McpTranslationMode;
import io.github.flexksx.toolpool.application.port.out.FindToolPort;
import io.github.flexksx.toolpool.application.port.out.LoadToolsPort;
import io.github.flexksx.toolpool.application.port.out.SearchToolsPort;
import io.github.flexksx.toolpool.application.port.out.SendToolCallPort;
import io.github.flexksx.toolpool.spring.ToolpoolAutoConfiguration;
import io.github.flexksx.toolpool.spring.mcp.McpServerDefaultProperties;
import io.modelcontextprotocol.server.McpSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@AutoConfiguration(before = ToolpoolAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    name = ToolpoolEmbeddedAutoConfiguration.ENABLED_PROPERTY,
    havingValue = "true")
public class ToolpoolEmbeddedAutoConfiguration {

  public static final String ENABLED_PROPERTY = "toolpool.embedded.enabled";
  private static final String HANDLER_MAPPING_BEAN = "requestMappingHandlerMapping";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ToolpoolEmbeddedAutoConfiguration.class);

  @Bean
  LocalServerAddress toolpoolLocalServerAddress(
      @Value("${server.servlet.context-path:}") String contextPath) {
    return new LocalServerAddress(contextPath);
  }

  @Bean
  @ConditionalOnMissingBean({LoadToolsPort.class, FindToolPort.class, SearchToolsPort.class})
  EmbeddedToolCatalogSource toolCatalogSource(
      LocalServerAddress address,
      @Qualifier(HANDLER_MAPPING_BEAN) RequestMappingHandlerMapping handlerMapping,
      @Value("${springdoc.api-docs.path:/v3/api-docs}") String apiDocsPath) {
    LOGGER
        .atInfo()
        .setMessage("Toolpool runs embedded and reads the local OpenAPI spec at {}")
        .addArgument(apiDocsPath)
        .log();
    return new EmbeddedToolCatalogSource(address, handlerMapping, apiDocsPath);
  }

  @Bean
  @ConditionalOnMissingBean
  SendToolCallPort toolCallExecutor(
      LocalServerAddress address, ObjectProvider<RestClient.Builder> restClientBuilders) {
    return new EmbeddedToolCallExecutor(
        address, restClientBuilders.getIfAvailable(RestClient::builder));
  }

  @Bean
  @ConditionalOnProperty(
      name = McpServerDefaultProperties.ENABLED_PROPERTY,
      havingValue = "true",
      matchIfMissing = true)
  EmbeddedToolRegistrar toolpoolEmbeddedToolRegistrar(
      McpToolUseCases useCases,
      McpSyncServer server,
      @Value("${toolpool.mode:METATOOLS}") McpTranslationMode mode) {
    return new EmbeddedToolRegistrar(useCases, server, mode);
  }
}
