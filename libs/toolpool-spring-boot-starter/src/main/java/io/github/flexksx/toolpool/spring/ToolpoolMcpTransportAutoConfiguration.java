package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.adapter.mcp.McpAccessTokenMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(before = McpServerStreamableHttpWebMvcAutoConfiguration.class)
@ConditionalOnProperty(
    name = McpServerDefaultProperties.PROTOCOL_PROPERTY,
    havingValue = McpServerDefaultProperties.STREAMABLE_PROTOCOL)
@EnableConfigurationProperties(McpServerStreamableHttpProperties.class)
public class ToolpoolMcpTransportAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
      @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
      McpServerStreamableHttpProperties serverProperties) {
    return WebMvcStreamableServerTransportProvider.builder()
        .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
        .mcpEndpoint(serverProperties.getMcpEndpoint())
        .keepAliveInterval(serverProperties.getKeepAliveInterval())
        .disallowDelete(serverProperties.isDisallowDelete())
        .contextExtractor(
            request ->
                McpAccessTokenMapper.toTransportContext(
                    request.headers().firstHeader(HttpHeaders.AUTHORIZATION)))
        .build();
  }
}
