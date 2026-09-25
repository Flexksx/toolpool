import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.application.UpstreamAccessTokenResolver;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.spring.TokenExchangeAccessTokenResolver;
import io.github.flexksx.toolpool.spring.ToolpoolAutoConfiguration;
import io.github.flexksx.toolpool.spring.ToolpoolSecurityAutoConfiguration;
import io.github.flexksx.toolpool.spring.ToolpoolTokenExchangeAutoConfiguration;
import io.github.flexksx.toolpool.testing.ToolpoolFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.client.endpoint.RestClientTokenExchangeTokenResponseClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

public class ToolpoolAuthAutoConfigurationTest {

  private static final AccessToken CALLER_TOKEN = new AccessToken("caller-token");

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ToolpoolTokenExchangeAutoConfiguration.class,
                  ToolpoolAutoConfiguration.class,
                  ToolpoolSecurityAutoConfiguration.class))
          .withPropertyValues(
              "toolpool.spec-location=" + ToolpoolFixtures.SAMPLE_SPEC,
              "toolpool.base-url=http://api.test");

  @Test
  void startInTheDefaultMode_forwardsNoTokenAndAddsNoSecurity() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed().doesNotHaveBean(SecurityFilterChain.class);
          assertThat(
                  context.getBean(UpstreamAccessTokenResolver.class).upstreamTokenFor(CALLER_TOKEN))
              .isNull();
        });
  }

  @Test
  void startInPassthroughMode_forwardsTheCallerTokenAndGuardsTheMcpEndpoint() {
    runner
        .withBean(JwtDecoder.class, ToolpoolAuthAutoConfigurationTest::rejectingDecoder)
        .withPropertyValues("toolpool.auth.mode=passthrough")
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasBean("toolpoolSecurityFilterChain");
              assertThat(
                      context
                          .getBean(UpstreamAccessTokenResolver.class)
                          .upstreamTokenFor(CALLER_TOKEN))
                  .isEqualTo(CALLER_TOKEN);
            });
  }

  @Test
  void startInTokenExchangeMode_usesTheTokenExchangeResolver() {
    runner
        .withBean(JwtDecoder.class, ToolpoolAuthAutoConfigurationTest::rejectingDecoder)
        .withPropertyValues(
            "toolpool.auth.mode=token-exchange",
            "toolpool.auth.token-exchange.token-uri=http://idp.test/token",
            "toolpool.auth.token-exchange.client-id=toolpool")
        .run(
            context ->
                assertThat(context)
                    .hasNotFailed()
                    .hasBean("toolpoolSecurityFilterChain")
                    .getBean(UpstreamAccessTokenResolver.class)
                    .isInstanceOf(TokenExchangeAccessTokenResolver.class));
  }

  @Test
  void startInTokenExchangeModeWithoutATokenUri_failsNamingTheMissingSetting() {
    runner
        .withBean(JwtDecoder.class, ToolpoolAuthAutoConfigurationTest::rejectingDecoder)
        .withPropertyValues(
            "toolpool.auth.mode=token-exchange", "toolpool.auth.token-exchange.client-id=toolpool")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("toolpool.auth.token-exchange.token-uri"));
  }

  @Test
  void startInPassthroughModeWithoutATokenCheck_failsNamingTheMissingSetting() {
    runner
        .withPropertyValues("toolpool.auth.mode=passthrough")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("spring.security.oauth2.resourceserver.jwt.issuer-uri"));
  }

  @Test
  void startInPassthroughModeWithoutTheResourceServerLibrary_failsNamingTheLibrary() {
    runner
        .withClassLoader(new FilteredClassLoader(BearerTokenAuthenticationEntryPoint.class))
        .withPropertyValues("toolpool.auth.mode=passthrough")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("spring-boot-starter-security-oauth2-resource-server"));
  }

  @Test
  void startInTokenExchangeModeWithoutTheClientLibrary_failsNamingTheLibrary() {
    runner
        .withClassLoader(new FilteredClassLoader(RestClientTokenExchangeTokenResponseClient.class))
        .withBean(JwtDecoder.class, ToolpoolAuthAutoConfigurationTest::rejectingDecoder)
        .withPropertyValues("toolpool.auth.mode=token-exchange")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("spring-boot-starter-security-oauth2-client"));
  }

  private static JwtDecoder rejectingDecoder() {
    return token -> {
      throw new org.springframework.security.oauth2.jwt.BadJwtException("test decoder");
    };
  }
}
