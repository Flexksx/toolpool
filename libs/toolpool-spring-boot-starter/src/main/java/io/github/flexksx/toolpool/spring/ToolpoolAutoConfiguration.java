package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.adapter.mcp.McpToolUseCases;
import io.github.flexksx.toolpool.adapter.mcp.McpTranslationMode;
import io.github.flexksx.toolpool.application.port.in.CallToolUseCase;
import io.github.flexksx.toolpool.application.port.in.DescribeToolUseCase;
import io.github.flexksx.toolpool.application.port.in.ListToolsUseCase;
import io.github.flexksx.toolpool.application.port.in.SearchToolsUseCase;
import io.github.flexksx.toolpool.application.port.out.FindToolPort;
import io.github.flexksx.toolpool.application.port.out.LoadToolsPort;
import io.github.flexksx.toolpool.application.port.out.ResolveUpstreamTokenPort;
import io.github.flexksx.toolpool.application.port.out.SearchToolsPort;
import io.github.flexksx.toolpool.application.port.out.SendToolCallPort;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.service.CallToolService;
import io.github.flexksx.toolpool.application.service.DescribeToolService;
import io.github.flexksx.toolpool.application.service.ListToolsService;
import io.github.flexksx.toolpool.application.service.SearchToolsService;
import io.github.flexksx.toolpool.spring.embedded.ToolpoolEmbeddedAutoConfiguration;
import io.github.flexksx.toolpool.spring.mcp.McpServerDefaultProperties;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ToolpoolAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ToolpoolAutoConfiguration.class);

  @Bean
  @ConditionalOnMissingBean(ListToolsUseCase.class)
  ListToolsService listToolsService(LoadToolsPort loadToolsPort) {
    return new ListToolsService(loadToolsPort);
  }

  @Bean
  @ConditionalOnMissingBean(SearchToolsUseCase.class)
  SearchToolsService searchToolsService(SearchToolsPort searchToolsPort) {
    return new SearchToolsService(searchToolsPort);
  }

  @Bean
  @ConditionalOnMissingBean(DescribeToolUseCase.class)
  DescribeToolService describeToolService(FindToolPort findToolPort) {
    return new DescribeToolService(findToolPort);
  }

  @Bean
  @ConditionalOnMissingBean(CallToolUseCase.class)
  CallToolService callToolService(
      FindToolPort findToolPort,
      SendToolCallPort sendToolCallPort,
      ResolveUpstreamTokenPort resolveUpstreamTokenPort) {
    return new CallToolService(findToolPort, sendToolCallPort, resolveUpstreamTokenPort);
  }

  @Bean
  McpToolUseCases mcpToolUseCases(
      ListToolsUseCase listToolsUseCase,
      SearchToolsUseCase searchToolsUseCase,
      DescribeToolUseCase describeToolUseCase,
      CallToolUseCase callToolUseCase) {
    return new McpToolUseCases(
        listToolsUseCase, searchToolsUseCase, describeToolUseCase, callToolUseCase);
  }

  @Bean
  @ConditionalOnProperty(
      name = McpServerDefaultProperties.ENABLED_PROPERTY,
      havingValue = "true",
      matchIfMissing = true)
  @ConditionalOnProperty(
      name = ToolpoolEmbeddedAutoConfiguration.ENABLED_PROPERTY,
      havingValue = "false",
      matchIfMissing = true)
  List<SyncToolSpecification> toolpoolToolSpecifications(
      McpToolUseCases useCases, @Value("${toolpool.mode:METATOOLS}") McpTranslationMode mode)
      throws ToolCatalogUnavailableException {
    List<SyncToolSpecification> specifications = mode.translationOf(useCases).toolSpecifications();
    LOGGER
        .atInfo()
        .setMessage("Toolpool runs in {} mode and exposes {} MCP tools")
        .addArgument(mode)
        .addArgument(specifications.size())
        .log();
    return specifications;
  }
}
