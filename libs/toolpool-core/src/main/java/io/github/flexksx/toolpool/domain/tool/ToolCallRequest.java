package io.github.flexksx.toolpool.domain.tool;

import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record ToolCallRequest(
    ToolName toolName,
    HttpTarget target,
    Map<String, Object> pathVariables,
    Map<String, List<String>> queryParameters,
    Map<String, String> headers,
    @Nullable Object body,
    @Nullable AccessToken accessToken) {

  public ToolCallRequest {
    Objects.requireNonNull(toolName, "A tool call request needs a tool name");
    Objects.requireNonNull(target, "A tool call request needs a target");
    pathVariables = orderedCopyOf(pathVariables);
    queryParameters = orderedCopyOf(queryParameters);
    headers = orderedCopyOf(headers);
  }

  public ToolCallRequest withAccessToken(@Nullable AccessToken token) {
    return new ToolCallRequest(
        toolName, target, pathVariables, queryParameters, headers, body, token);
  }

  private static <V> Map<String, V> orderedCopyOf(Map<String, V> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }
}
