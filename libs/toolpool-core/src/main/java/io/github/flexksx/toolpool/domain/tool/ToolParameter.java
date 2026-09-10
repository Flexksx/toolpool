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
  }

  public static ToolParameter body(
      boolean required, JsonSchema schema, @Nullable String description) {
    return new ToolParameter(BODY_NAME, ParameterLocation.BODY, required, schema, description);
  }

  public JsonSchema describedSchema() {
    return schema.withDescription(description);
  }
}
