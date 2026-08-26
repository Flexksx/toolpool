package io.github.flexksx.domain.tool;

import io.github.flexksx.domain.http.ParameterLocation;
import io.github.flexksx.domain.schema.JsonSchema;
import org.jspecify.annotations.Nullable;

public record ToolParameter(
    String name,
    ParameterLocation location,
    boolean required,
    JsonSchema schema,
    @Nullable String description) {

  public ToolParameter {
    if (name.isBlank()) {
      throw new IllegalArgumentException("A parameter name cannot be blank");
    }
  }

  public JsonSchema describedSchema() {
    return schema.withDescription(description);
  }
}
