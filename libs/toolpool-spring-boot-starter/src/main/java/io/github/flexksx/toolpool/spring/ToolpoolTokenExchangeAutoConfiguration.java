package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.application.UpstreamAccessTokenResolver;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.oauth2.client.endpoint.RestClientTokenExchangeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

@AutoConfiguration(before = ToolpoolAutoConfiguration.class)
@ConditionalOnClass(RestClientTokenExchangeTokenResponseClient.class)
@Conditional(ToolpoolAuthConditions.TokenExchangeSelected.class)
@EnableConfigurationProperties(ToolpoolAuthProperties.class)
public class ToolpoolTokenExchangeAutoConfiguration {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ToolpoolTokenExchangeAutoConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  UpstreamAccessTokenResolver upstreamAccessTokenResolver(ToolpoolAuthProperties auth) {
    ToolpoolAuthProperties.TokenExchange settings = auth.tokenExchange();
    ClientRegistration registration =
        TokenExchangeAccessTokenResolver.toClientRegistration(settings);
    LOGGER
        .atInfo()
        .setMessage("Toolpool exchanges each caller token at {} for an upstream token for {}")
        .addArgument(settings.tokenUri())
        .addArgument(settings.audience())
        .log();
    return new TokenExchangeAccessTokenResolver(
        registration,
        settings.audience(),
        new RestClientTokenExchangeTokenResponseClient(),
        Clock.systemUTC());
  }
}
