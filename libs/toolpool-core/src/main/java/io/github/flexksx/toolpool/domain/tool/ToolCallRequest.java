package io.github.flexksx.toolpool.domain.tool;

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
    @Nullable Object body) {

  public ToolCallRequest {
    Objects.requireNonNull(toolName, "A tool call request needs a tool name");
    Objects.requireNonNull(target, "A tool call request needs a target");
    pathVariables = orderedCopyOf(pathVariables);
    queryParameters = orderedCopyOf(queryParameters);
    headers = orderedCopyOf(headers);
  }

  private static <V> Map<String, V> orderedCopyOf(Map<String, V> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }
}
