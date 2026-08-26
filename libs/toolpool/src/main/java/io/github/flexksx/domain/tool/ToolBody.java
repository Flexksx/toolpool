package io.github.flexksx.domain.tool;

import io.github.flexksx.domain.schema.JsonSchema;
import org.jspecify.annotations.Nullable;

public record ToolBody(boolean required, JsonSchema schema, @Nullable String description) {

  public JsonSchema describedSchema() {
    return schema.withDescription(description);
  }
}
