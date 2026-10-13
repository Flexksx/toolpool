package io.github.flexksx.toolpool.application.port.out;

import io.github.flexksx.toolpool.domain.auth.AccessToken;
import org.jspecify.annotations.Nullable;

public interface ResolveUpstreamTokenPort {

  static ResolveUpstreamTokenPort none() {
    return _ -> null;
  }

  static ResolveUpstreamTokenPort passthrough() {
    return callerToken -> callerToken;
  }

  @Nullable AccessToken resolveUpstreamToken(@Nullable AccessToken callerToken);
}
