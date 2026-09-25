package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.application.UpstreamAccessTokenResolver;
import io.github.flexksx.toolpool.application.UpstreamAccessTokenUnavailableException;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.endpoint.RestClientTokenExchangeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.TokenExchangeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;

public final class TokenExchangeAccessTokenResolver implements UpstreamAccessTokenResolver {

  static final String REGISTRATION_ID = "toolpool-token-exchange";
  private static final String AUDIENCE_PARAMETER = "audience";
  private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);

  private final ClientRegistration registration;
  private final RestClientTokenExchangeTokenResponseClient tokenClient;
  private final Clock clock;
  private final Map<String, OAuth2AccessToken> tokensByCallerToken = new ConcurrentHashMap<>();

  public TokenExchangeAccessTokenResolver(
      ClientRegistration registration,
      @Nullable String audience,
      RestClientTokenExchangeTokenResponseClient tokenClient,
      Clock clock) {
    this.registration = registration;
    this.tokenClient = tokenClient;
    this.clock = clock;
    if (audience != null && !audience.isBlank()) {
      tokenClient.setParametersCustomizer(
          parameters -> parameters.set(AUDIENCE_PARAMETER, audience));
    }
  }

  static ClientRegistration toClientRegistration(ToolpoolAuthProperties.TokenExchange settings) {
    String tokenUri = settings.tokenUri();
    String clientId = settings.clientId();
    if (tokenUri == null || tokenUri.isBlank() || clientId == null || clientId.isBlank()) {
      throw new IllegalStateException(
          "toolpool.auth.mode=token-exchange needs toolpool.auth.token-exchange.token-uri"
              + " and toolpool.auth.token-exchange.client-id");
    }
    return ClientRegistration.withRegistrationId(REGISTRATION_ID)
        .clientId(clientId)
        .clientSecret(settings.clientSecret())
        .clientAuthenticationMethod(
            new ClientAuthenticationMethod(settings.clientAuthenticationMethod()))
        .authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
        .tokenUri(tokenUri)
        .scope(settings.scopes())
        .build();
  }

  @Override
  public AccessToken upstreamTokenFor(@Nullable AccessToken callerToken) {
    if (callerToken == null) {
      throw new UpstreamAccessTokenUnavailableException(
          "Toolpool needs the caller's access token to exchange it for an upstream token");
    }
    OAuth2AccessToken cached = tokensByCallerToken.get(callerToken.value());
    if (cached != null && isFresh(cached)) {
      return new AccessToken(cached.getTokenValue());
    }
    OAuth2AccessToken exchanged = exchange(callerToken);
    tokensByCallerToken.values().removeIf(token -> !isFresh(token));
    if (isFresh(exchanged)) {
      tokensByCallerToken.put(callerToken.value(), exchanged);
    }
    return new AccessToken(exchanged.getTokenValue());
  }

  private OAuth2AccessToken exchange(AccessToken callerToken) {
    OAuth2AccessToken subjectToken =
        new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, callerToken.value(), null, null);
    try {
      return tokenClient
          .getTokenResponse(new TokenExchangeGrantRequest(registration, subjectToken, null))
          .getAccessToken();
    } catch (OAuth2AuthorizationException failure) {
      throw new UpstreamAccessTokenUnavailableException(
          "The authorization server refused to exchange the caller's token: "
              + failure.getError().getErrorCode(),
          failure);
    }
  }

  private boolean isFresh(OAuth2AccessToken token) {
    Instant expiresAt = token.getExpiresAt();
    return expiresAt != null && clock.instant().plus(EXPIRY_MARGIN).isBefore(expiresAt);
  }
}
