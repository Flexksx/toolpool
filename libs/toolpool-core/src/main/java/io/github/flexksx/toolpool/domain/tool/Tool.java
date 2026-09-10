package io.github.flexksx.toolpool.domain.tool;

import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record Tool(
    ToolName name,
    HttpTarget target,
    ToolDocumentation documentation,
    List<ToolParameter> parameters) {

  public Tool {
    parameters = List.copyOf(parameters);
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
    ToolCallRequest.Builder request = ToolCallRequest.builder(name, target);

    for (ToolParameter parameter : parameters) {
      Object value = given.get(parameter.name());
      if (value == null) {
        if (parameter.required()) {
          throw new MissingRequiredArgumentException(name, parameter);
        }
        continue;
      }
      switch (parameter.location()) {
        case PATH -> request.pathVariable(parameter.name(), value);
        case QUERY -> request.queryParameter(parameter.name(), value);
        case HEADER -> request.header(parameter.name(), value);
        case BODY -> request.body(value);
      }
    }

    return request.build();
  }

  private String searchableText() {
    return (name.value() + " " + documentation.searchableText()).toLowerCase(Locale.ROOT);
  }
}
