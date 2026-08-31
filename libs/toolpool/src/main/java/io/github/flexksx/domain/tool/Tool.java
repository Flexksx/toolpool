package io.github.flexksx.domain.tool;

import io.github.flexksx.domain.http.HttpTarget;
import io.github.flexksx.domain.schema.JsonSchema;
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
    List<ToolParameter> parameters,
    @Nullable ToolBody body) {

  public static final String BODY_ARGUMENT = "body";

  private static final String OBJECT_TYPE = "object";
  private static final String TYPE_KEYWORD = "type";
  private static final String PROPERTIES_KEYWORD = "properties";
  private static final String REQUIRED_KEYWORD = "required";

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
    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> requiredProperties = new ArrayList<>();

    for (ToolParameter parameter : parameters) {
      properties.put(parameter.name(), parameter.describedSchema().asMap());
      if (parameter.required()) {
        requiredProperties.add(parameter.name());
      }
    }

    if (body != null) {
      properties.put(BODY_ARGUMENT, body.describedSchema().asMap());
      if (body.required()) {
        requiredProperties.add(BODY_ARGUMENT);
      }
    }

    Map<String, Object> inputSchema = new LinkedHashMap<>();
    inputSchema.put(TYPE_KEYWORD, OBJECT_TYPE);
    inputSchema.put(PROPERTIES_KEYWORD, properties);
    inputSchema.put(REQUIRED_KEYWORD, requiredProperties);
    return new JsonSchema(inputSchema);
  }

  public ToolCall bind(@Nullable Map<String, Object> arguments) {
    Map<String, Object> given = arguments == null ? Map.of() : arguments;
    Map<String, Object> pathVariables = new LinkedHashMap<>();
    Map<String, List<String>> queryParameters = new LinkedHashMap<>();
    Map<String, String> headers = new LinkedHashMap<>();

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
      }
    }

    Object givenBody = given.get(BODY_ARGUMENT);
    if (givenBody == null && body != null && body.required()) {
      throw new MissingRequiredArgumentException(name, BODY_ARGUMENT);
    }

    return new ToolCall(name, target, pathVariables, queryParameters, headers, givenBody);
  }

  private String searchableText() {
    return (name.value() + " " + documentation.searchableText()).toLowerCase(Locale.ROOT);
  }
}
