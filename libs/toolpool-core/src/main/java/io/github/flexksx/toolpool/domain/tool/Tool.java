package io.github.flexksx.toolpool.domain.tool;

import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record Tool(
    ToolName name,
    HttpTarget target,
    ToolDocumentation documentation,
    List<ToolParameter> parameters) {

  public Tool {
    parameters = List.copyOf(parameters);
    requireDistinctNames(name, parameters);
  }

  public String description() {
    String text = documentation.text();
    return text.isBlank() ? target.describe() : text;
  }

  public boolean matches(@Nullable String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    return searchableText().contains(query.toLowerCase(Locale.ROOT));
  }

  public JsonSchema inputSchema() {
    Map<String, JsonSchema> properties = new LinkedHashMap<>();
    List<String> requiredProperties = new ArrayList<>();

    for (ToolParameter parameter : parameters) {
      properties.put(parameter.name(), parameter.describedSchema());
      if (parameter.required()) {
        requiredProperties.add(parameter.name());
      }
    }

    return JsonSchema.objectOf(properties, requiredProperties);
  }

  public ToolCallRequest requestFor(@Nullable Map<String, Object> arguments) {
    Map<String, Object> given = arguments == null ? Map.of() : arguments;
    Map<String, Object> pathVariables = new LinkedHashMap<>();
    Map<String, List<String>> queryParameters = new LinkedHashMap<>();
    Map<String, String> headers = new LinkedHashMap<>();
    Object body = null;

    for (ToolParameter parameter : parameters) {
      Object value = given.get(parameter.name());
      if (value == null) {
        if (parameter.required()) {
          throw new MissingRequiredArgumentException(name, parameter);
        }
        continue;
      }
      switch (parameter.location()) {
        case PATH -> pathVariables.put(parameter.name(), value);
        case QUERY -> queryParameters.put(parameter.name(), List.of(String.valueOf(value)));
        case HEADER -> headers.put(parameter.name(), String.valueOf(value));
        case BODY -> body = value;
      }
    }

    return new ToolCallRequest(name, target, pathVariables, queryParameters, headers, body, null);
  }

  private static void requireDistinctNames(ToolName name, List<ToolParameter> parameters) {
    Set<String> seenNames = new HashSet<>();
    for (ToolParameter parameter : parameters) {
      if (!seenNames.add(parameter.name())) {
        throw new IllegalArgumentException(
            "Tool " + name.value() + " declares the parameter " + parameter.name() + " twice");
      }
    }
  }

  private String searchableText() {
    return (name.value() + " " + documentation.searchableText()).toLowerCase(Locale.ROOT);
  }
}
