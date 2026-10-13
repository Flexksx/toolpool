package io.github.flexksx.toolpool.spring.server;

import io.github.flexksx.toolpool.adapter.mcp.McpToolUseCases;
import io.github.flexksx.toolpool.adapter.mcp.McpTranslationMode;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.spring.auth.ToolpoolSecurityAutoConfiguration;
import io.github.flexksx.toolpool.spring.mcp.ToolpoolMcpTransportAutoConfiguration;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.function.support.HandlerFunctionAdapter;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * Holds the beans of the Toolpool server. The server serves only the MCP endpoint: its dispatcher
 * does not look up handlers in the main application, so the main application's routes stay on the
 * main port.
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration({
  McpServerJsonMapperAutoConfiguration.class,
  ToolpoolMcpTransportAutoConfiguration.class,
  McpServerStreamableHttpWebMvcAutoConfiguration.class,
  McpServerAutoConfiguration.class
})
class ToolpoolServerConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ToolpoolServerConfiguration.class);

  @Bean
  DispatcherServlet dispatcherServlet() {
    DispatcherServlet dispatcherServlet = new DispatcherServlet();
    dispatcherServlet.setDetectAllHandlerMappings(false);
    dispatcherServlet.setDetectAllHandlerAdapters(false);
    dispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
    dispatcherServlet.setDetectAllViewResolvers(false);
    return dispatcherServlet;
  }

  @Bean(DispatcherServlet.HANDLER_MAPPING_BEAN_NAME)
  RouterFunctionMapping handlerMapping() {
    RouterFunctionMapping handlerMapping = new RouterFunctionMapping();
    List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
    HttpMessageConverters.forServer().registerDefaults().build().forEach(messageConverters::add);
    handlerMapping.setMessageConverters(messageConverters);
    return handlerMapping;
  }

  @Bean(DispatcherServlet.HANDLER_ADAPTER_BEAN_NAME)
  HandlerFunctionAdapter handlerAdapter() {
    return new HandlerFunctionAdapter();
  }

  @Bean(DispatcherServlet.HANDLER_EXCEPTION_RESOLVER_BEAN_NAME)
  HandlerExceptionResolver handlerExceptionResolver() {
    return new DefaultHandlerExceptionResolver();
  }

  @Bean
  List<SyncToolSpecification> toolpoolToolSpecifications(
      McpToolUseCases useCases, @Value("${toolpool.mode:METATOOLS}") McpTranslationMode mode)
      throws ToolCatalogUnavailableException {
    List<SyncToolSpecification> specifications = mode.translationOf(useCases).toolSpecifications();
    LOGGER
        .atInfo()
        .setMessage("Toolpool server runs in {} mode and exposes {} MCP tools")
        .addArgument(mode)
        .addArgument(specifications.size())
        .log();
    return specifications;
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(
      name =
          "org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint")
  @ImportAutoConfiguration(ToolpoolSecurityAutoConfiguration.class)
  static class SecurityConfiguration {}
}
