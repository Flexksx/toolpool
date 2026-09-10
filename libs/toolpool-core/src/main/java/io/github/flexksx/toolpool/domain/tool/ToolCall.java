package io.github.flexksx.toolpool.domain.tool;

import io.github.flexksx.toolpool.domain.http.HttpTarget;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record ToolCall(
    ToolName toolName,
    HttpTarget target,
    Map<String, Object> pathVariables,
    Map<String, List<String>> queryParameters,
    Map<String, String> headers,
    @Nullable Object body) {

  public ToolCall {
    pathVariables = orderedCopyOf(pathVariables);
    queryParameters = orderedCopyOf(queryParameters);
    headers = orderedCopyOf(headers);
  }

  private static <V> Map<String, V> orderedCopyOf(Map<String, V> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }
}
