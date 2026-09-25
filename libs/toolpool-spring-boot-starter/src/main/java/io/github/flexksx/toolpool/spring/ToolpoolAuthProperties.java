package io.github.flexksx.toolpool.spring;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(ToolpoolAuthProperties.PREFIX)
public record ToolpoolAuthProperties(
    @DefaultValue("none") ToolpoolAuthMode mode,
    @Nullable String authorizationServer,
    @DefaultValue TokenExchange tokenExchange) {

  static final String PREFIX = "toolpool.auth";
  static final String MODE_PROPERTY = PREFIX + ".mode";

  public record TokenExchange(
      @Nullable String tokenUri,
      @Nullable String clientId,
      @Nullable String clientSecret,
      @Nullable String audience,
      @DefaultValue List<String> scopes,
      @DefaultValue("client_secret_basic") String clientAuthenticationMethod) {}
}
