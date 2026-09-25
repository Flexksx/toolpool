package io.github.flexksx.toolpool.domain.tool;

import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import org.jspecify.annotations.Nullable;

public record ToolParameter(
    String name,
    ParameterLocation location,
    boolean required,
    JsonSchema schema,
    @Nullable String description) {

  public static final String BODY_NAME = "body";

  public ToolParameter {
    if (name.isBlank()) {
      throw new IllegalArgumentException("A parameter name cannot be blank");
    }
    if (location == ParameterLocation.BODY && !BODY_NAME.equals(name)) {
      throw new IllegalArgumentException(
          "A body parameter must be named " + BODY_NAME + ", not " + name);
    }
  }

  public static ToolParameter inPath(String name, JsonSchema schema) {
    return new ToolParameter(name, ParameterLocation.PATH, false, schema, null);
  }

  public static ToolParameter inQuery(String name, JsonSchema schema) {
    return new ToolParameter(name, ParameterLocation.QUERY, false, schema, null);
  }

  public static ToolParameter inHeader(String name, JsonSchema schema) {
    return new ToolParameter(name, ParameterLocation.HEADER, false, schema, null);
  }

  public static ToolParameter inBody(JsonSchema schema) {
    return new ToolParameter(BODY_NAME, ParameterLocation.BODY, false, schema, null);
  }

  public ToolParameter withRequired(boolean value) {
    return new ToolParameter(name, location, value, schema, description);
  }

  public ToolParameter withDescription(@Nullable String value) {
    return new ToolParameter(name, location, required, schema, value);
  }

  public JsonSchema describedSchema() {
    return schema.withDescription(description);
  }
}
