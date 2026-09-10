package io.github.flexksx.toolpool.domain.tool;

import io.github.flexksx.toolpool.domain.http.HttpTarget;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record ToolCallRequest(
    ToolName toolName,
    HttpTarget target,
    Map<String, Object> pathVariables,
    Map<String, List<String>> queryParameters,
    Map<String, String> headers,
    @Nullable Object body) {

  public ToolCallRequest {
    pathVariables = orderedCopyOf(pathVariables);
    queryParameters = orderedCopyOf(queryParameters);
    headers = orderedCopyOf(headers);
  }

  public static Builder builder(ToolName toolName, HttpTarget target) {
    return new Builder(toolName, target);
  }

  private static <V> Map<String, V> orderedCopyOf(Map<String, V> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }

  public static final class Builder {

    private final ToolName toolName;
    private final HttpTarget target;
    private final Map<String, Object> pathVariables = new LinkedHashMap<>();
    private final Map<String, List<String>> queryParameters = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();

    private @Nullable Object body;

    private Builder(ToolName toolName, HttpTarget target) {
      this.toolName = toolName;
      this.target = target;
    }

    public Builder pathVariable(String name, Object value) {
      pathVariables.put(name, value);
      return this;
    }

    public Builder queryParameter(String name, Object value) {
      queryParameters.put(name, List.of(String.valueOf(value)));
      return this;
    }

    public Builder header(String name, Object value) {
      headers.put(name, String.valueOf(value));
      return this;
    }

    public Builder body(Object value) {
      body = value;
      return this;
    }

    public ToolCallRequest build() {
      return new ToolCallRequest(toolName, target, pathVariables, queryParameters, headers, body);
    }
  }
}
