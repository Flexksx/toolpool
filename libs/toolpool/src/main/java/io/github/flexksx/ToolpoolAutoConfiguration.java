package io.github.flexksx;

import io.github.flexksx.mcp.McpDirectTools;
import io.github.flexksx.mcp.McpGatewayMetatools;
import io.github.flexksx.openapi.CachingOpenApiSpecRepository;
import io.github.flexksx.openapi.OpenApiSpecReader;
import io.github.flexksx.openapi.OpenApiSpecRepository;
import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ToolpoolAutoConfiguration {

  private static final String MODE_PROPERTY = "toolpool.mode";
  private static final String MODE_METATOOLS = "metatools";
  private static final String MODE_DIRECT = "direct";

  @Bean
  @ConditionalOnMissingBean
  OpenApiSpecReader openApiSpecReader() {
    return new SwaggerOpenApiSpecReader();
  }

  @Bean
  @ConditionalOnMissingBean
  OpenApiSpecRepository openApiSpecRepository(
      OpenApiSpecReader specReader,
      @Value("${toolpool.refresh-interval:5m}") Duration refreshInterval) {
    return new CachingOpenApiSpecRepository(specReader, refreshInterval, Clock.systemUTC());
  }

  @Bean
  @ConditionalOnProperty(name = MODE_PROPERTY, havingValue = MODE_METATOOLS, matchIfMissing = true)
  McpGatewayMetatools mcpGatewayMetatools(
      OpenApiSpecRepository specRepository,
      @Value("${toolpool.spec-location}") String specLocation) {
    return new McpGatewayMetatools(specRepository, specLocation);
  }

  @Bean
  @ConditionalOnProperty(name = MODE_PROPERTY, havingValue = MODE_DIRECT)
  McpDirectTools mcpDirectTools() {
    return new McpDirectTools();
  }

  @Bean
  @ConditionalOnProperty(name = MODE_PROPERTY, havingValue = MODE_DIRECT)
  List<SyncToolSpecification> directToolSpecifications(McpDirectTools directTools) {
    return directTools.toolSpecifications();
  }
}
