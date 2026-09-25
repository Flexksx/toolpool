package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class McpAccessTokenMapper {

  static final String CALLER_TOKEN_KEY = "toolpool.callerToken";
  private static final String BEARER_SCHEME = "Bearer ";

  private McpAccessTokenMapper() {}

  public static McpTransportContext toTransportContext(@Nullable String authorizationHeader) {
    if (authorizationHeader == null
        || !authorizationHeader.regionMatches(true, 0, BEARER_SCHEME, 0, BEARER_SCHEME.length())) {
      return McpTransportContext.EMPTY;
    }
    String token = authorizationHeader.substring(BEARER_SCHEME.length()).strip();
    if (token.isEmpty()) {
      return McpTransportContext.EMPTY;
    }
    return McpTransportContext.create(Map.of(CALLER_TOKEN_KEY, new AccessToken(token)));
  }

  public static @Nullable AccessToken toCallerToken(@Nullable McpSyncServerExchange exchange) {
    if (exchange == null) {
      return null;
    }
    return exchange.transportContext().get(CALLER_TOKEN_KEY) instanceof AccessToken token
        ? token
        : null;
  }
}
