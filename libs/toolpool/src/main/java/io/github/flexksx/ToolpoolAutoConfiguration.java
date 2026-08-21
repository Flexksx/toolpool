package io.github.flexksx;

import io.github.flexksx.mcp.McpGatewayMetatools;
import io.github.flexksx.openapi.CachingOpenApiSpecRepository;
import io.github.flexksx.openapi.OpenApiSpecReader;
import io.github.flexksx.openapi.OpenApiSpecRepository;
import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(McpGatewayMetatools.class)
public class ToolpoolAutoConfiguration {

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
}
