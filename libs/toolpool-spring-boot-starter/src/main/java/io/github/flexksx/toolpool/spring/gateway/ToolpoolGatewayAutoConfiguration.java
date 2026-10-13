package io.github.flexksx.toolpool.spring.gateway;

import io.github.flexksx.toolpool.adapter.openapi.OpenApiToolCatalogSource;
import io.github.flexksx.toolpool.application.port.out.FindToolPort;
import io.github.flexksx.toolpool.application.port.out.LoadToolsPort;
import io.github.flexksx.toolpool.application.port.out.SearchToolsPort;
import io.github.flexksx.toolpool.application.port.out.SendToolCallPort;
import io.github.flexksx.toolpool.spring.RestClientToolCallExecutor;
import io.github.flexksx.toolpool.spring.ToolpoolAutoConfiguration;
import io.github.flexksx.toolpool.spring.embedded.ToolpoolEmbeddedAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration(
    after = ToolpoolEmbeddedAutoConfiguration.class,
    before = ToolpoolAutoConfiguration.class)
public class ToolpoolGatewayAutoConfiguration {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ToolpoolGatewayAutoConfiguration.class);
  private static final String OBSERVED_CLIENT = "context-managed";
  private static final String UNMANAGED_CLIENT = "standalone";

  @Bean
  @ConditionalOnMissingBean({LoadToolsPort.class, FindToolPort.class, SearchToolsPort.class})
  OpenApiToolCatalogSource toolCatalogSource(
      @Value("${toolpool.spec-location}") String specLocation) {
    LOGGER
        .atInfo()
        .setMessage("Toolpool reads its OpenAPI spec from {}")
        .addArgument(specLocation)
        .log();
    return new OpenApiToolCatalogSource(specLocation);
  }

  @Bean
  @ConditionalOnMissingBean
  SendToolCallPort toolCallExecutor(
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
}
