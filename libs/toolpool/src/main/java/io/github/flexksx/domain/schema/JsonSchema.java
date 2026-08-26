package io.github.flexksx.domain.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record JsonSchema(Map<String, Object> asMap) {

  private static final String DESCRIPTION_KEYWORD = "description";

  public JsonSchema {
    asMap = Collections.unmodifiableMap(new LinkedHashMap<>(asMap));
  }

  public static JsonSchema empty() {
    return new JsonSchema(Map.of());
  }

  public JsonSchema withDescription(@Nullable String description) {
    if (description == null || description.isBlank() || asMap.containsKey(DESCRIPTION_KEYWORD)) {
      return this;
    }
    Map<String, Object> described = new LinkedHashMap<>(asMap);
    described.put(DESCRIPTION_KEYWORD, description);
    return new JsonSchema(described);
  }
}
