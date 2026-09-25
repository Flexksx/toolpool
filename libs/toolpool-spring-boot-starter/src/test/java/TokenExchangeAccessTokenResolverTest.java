import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.flexksx.toolpool.application.UpstreamAccessTokenUnavailableException;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.spring.TokenExchangeAccessTokenResolver;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.RestClientTokenExchangeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class TokenExchangeAccessTokenResolverTest {

  private static final String TOKEN_URI = "http://idp.test/token";
  private static final String CLIENT_ID = "toolpool";
  private static final String CLIENT_SECRET = "toolpool-secret";
  private static final String AUDIENCE = "upstream-api";
  private static final AccessToken CALLER_TOKEN = new AccessToken("caller-token");
  private static final long LIFETIME_SECONDS = 300;

  private final MutableClock clock = new MutableClock(Instant.now());

  private MockRestServiceServer idp;
  private TokenExchangeAccessTokenResolver resolver;

  @BeforeEach
  void bindResolverToAMockedAuthorizationServer() {
    RestClient.Builder restClientBuilder =
        RestClient.builder()
            .configureMessageConverters(
                converters -> {
                  converters.addCustomConverter(new FormHttpMessageConverter());
                  converters.addCustomConverter(
                      new OAuth2AccessTokenResponseHttpMessageConverter());
                })
            .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler());
    idp = MockRestServiceServer.bindTo(restClientBuilder).build();
    RestClientTokenExchangeTokenResponseClient tokenClient =
        new RestClientTokenExchangeTokenResponseClient();
    tokenClient.setRestClient(restClientBuilder.build());
    resolver = new TokenExchangeAccessTokenResolver(registration(), AUDIENCE, tokenClient, clock);
  }

  @Test
  void upstreamTokenFor_sendsAnRfc8693ExchangeAndAnswersTheExchangedToken() {
    idp.expect(requestTo(TOKEN_URI))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuthorization()))
        .andExpect(
            content()
                .formDataContains(
                    Map.of(
                        "grant_type", "urn:ietf:params:oauth:grant-type:token-exchange",
                        "subject_token", CALLER_TOKEN.value(),
                        "subject_token_type", "urn:ietf:params:oauth:token-type:access_token",
                        "requested_token_type", "urn:ietf:params:oauth:token-type:access_token",
                        "audience", AUDIENCE)))
        .andRespond(tokenResponse("upstream-1"));

    AccessToken upstream = resolver.upstreamTokenFor(CALLER_TOKEN);

    idp.verify();
    assertThat(upstream).isEqualTo(new AccessToken("upstream-1"));
  }

  @Test
  void upstreamTokenForTheSameCallerTokenTwice_exchangesItOnce() {
    idp.expect(once(), requestTo(TOKEN_URI)).andRespond(tokenResponse("upstream-1"));

    resolver.upstreamTokenFor(CALLER_TOKEN);
    AccessToken second = resolver.upstreamTokenFor(CALLER_TOKEN);

    idp.verify();
    assertThat(second).isEqualTo(new AccessToken("upstream-1"));
  }

  @Test
  void upstreamTokenForACallerTokenWhoseExchangedTokenExpired_exchangesItAgain() {
    idp.expect(requestTo(TOKEN_URI)).andRespond(tokenResponse("upstream-1"));
    idp.expect(requestTo(TOKEN_URI)).andRespond(tokenResponse("upstream-2"));

    resolver.upstreamTokenFor(CALLER_TOKEN);
    clock.advance(Duration.ofSeconds(LIFETIME_SECONDS));
    AccessToken renewed = resolver.upstreamTokenFor(CALLER_TOKEN);

    idp.verify();
    assertThat(renewed).isEqualTo(new AccessToken("upstream-2"));
  }

  @Test
  void upstreamTokenForTwoCallerTokens_exchangesEachOne() {
    idp.expect(requestTo(TOKEN_URI))
        .andExpect(content().formDataContains(Map.of("subject_token", "caller-a")))
        .andRespond(tokenResponse("upstream-a"));
    idp.expect(requestTo(TOKEN_URI))
        .andExpect(content().formDataContains(Map.of("subject_token", "caller-b")))
        .andRespond(tokenResponse("upstream-b"));

    AccessToken first = resolver.upstreamTokenFor(new AccessToken("caller-a"));
    AccessToken second = resolver.upstreamTokenFor(new AccessToken("caller-b"));

    idp.verify();
    assertThat(first).isEqualTo(new AccessToken("upstream-a"));
    assertThat(second).isEqualTo(new AccessToken("upstream-b"));
  }

  @Test
  void upstreamTokenForARefusedExchange_throwsNamingTheErrorButNotTheToken() {
    idp.expect(requestTo(TOKEN_URI))
        .andRespond(
            withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"invalid_grant\"}"));

    assertThatThrownBy(() -> resolver.upstreamTokenFor(CALLER_TOKEN))
        .isInstanceOf(UpstreamAccessTokenUnavailableException.class)
        .hasMessageContaining("invalid_grant")
        .hasMessageNotContaining(CALLER_TOKEN.value());
  }

  @Test
  void upstreamTokenForNoCallerToken_throwsWithoutCallingTheAuthorizationServer() {
    assertThatThrownBy(() -> resolver.upstreamTokenFor(null))
        .isInstanceOf(UpstreamAccessTokenUnavailableException.class)
        .hasMessageContaining("caller's access token");

    idp.verify();
  }

  private static ClientRegistration registration() {
    return ClientRegistration.withRegistrationId("test")
        .clientId(CLIENT_ID)
        .clientSecret(CLIENT_SECRET)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
        .tokenUri(TOKEN_URI)
        .build();
  }

  private static String basicAuthorization() {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
  }

  private static org.springframework.test.web.client.ResponseCreator tokenResponse(String token) {
    return withSuccess(
        "{\"access_token\":\""
            + token
            + "\",\"token_type\":\"Bearer\",\"expires_in\":"
            + LIFETIME_SECONDS
            + "}",
        MediaType.APPLICATION_JSON);
  }

  private static final class MutableClock extends Clock {

    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
