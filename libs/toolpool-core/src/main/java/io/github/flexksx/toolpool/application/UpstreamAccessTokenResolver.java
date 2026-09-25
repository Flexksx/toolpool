package io.github.flexksx.toolpool.application;

import io.github.flexksx.toolpool.domain.auth.AccessToken;
import org.jspecify.annotations.Nullable;

public interface UpstreamAccessTokenResolver {

  static UpstreamAccessTokenResolver none() {
    return callerToken -> null;
  }

  static UpstreamAccessTokenResolver passthrough() {
    return callerToken -> callerToken;
  }

  @Nullable AccessToken upstreamTokenFor(@Nullable AccessToken callerToken);
}
