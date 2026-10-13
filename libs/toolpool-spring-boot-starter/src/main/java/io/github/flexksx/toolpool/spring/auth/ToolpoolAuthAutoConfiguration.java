package io.github.flexksx.toolpool.spring.auth;

import io.github.flexksx.toolpool.application.port.out.ResolveUpstreamTokenPort;
import io.github.flexksx.toolpool.spring.ToolpoolAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

@AutoConfiguration(
    after = ToolpoolTokenExchangeAutoConfiguration.class,
    before = ToolpoolAutoConfiguration.class)
@EnableConfigurationProperties(ToolpoolAuthProperties.class)
public class ToolpoolAuthAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ToolpoolAuthAutoConfiguration.class);
  private static final String RESOURCE_SERVER_CLASS =
      "org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint";

  ToolpoolAuthAutoConfiguration(ToolpoolAuthProperties auth, ResourceLoader resourceLoader) {
    if (auth.mode().requiresCallerToken()
        && !ClassUtils.isPresent(RESOURCE_SERVER_CLASS, resourceLoader.getClassLoader())) {
      throw new IllegalStateException(
          "toolpool.auth.mode="
              + auth.mode()
              + " needs spring-boot-starter-security-oauth2-resource-server on the classpath,"
              + " so that Toolpool can check each caller token");
    }
  }

  @Bean
  @ConditionalOnMissingBean
  ResolveUpstreamTokenPort upstreamAccessTokenResolver(ToolpoolAuthProperties auth) {
    LOGGER
        .atInfo()
        .setMessage("Toolpool resolves upstream access tokens in {} mode")
        .addArgument(auth.mode())
        .log();
    return switch (auth.mode()) {
      case NONE -> ResolveUpstreamTokenPort.none();
      case PASSTHROUGH -> ResolveUpstreamTokenPort.passthrough();
      case TOKEN_EXCHANGE ->
          throw new IllegalStateException(
              "toolpool.auth.mode=token-exchange needs"
                  + " spring-boot-starter-security-oauth2-client on the classpath");
    };
  }
}
