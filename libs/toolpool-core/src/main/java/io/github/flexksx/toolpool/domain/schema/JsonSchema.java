package io.github.flexksx.toolpool.domain.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record JsonSchema(Map<String, Object> asMap) {

  private static final String DESCRIPTION_KEYWORD = "description";
  private static final String OBJECT_TYPE = "object";
  private static final String PROPERTIES_KEYWORD = "properties";
  private static final String REQUIRED_KEYWORD = "required";
  private static final String STRING_TYPE = "string";
  private static final String TYPE_KEYWORD = "type";

  public JsonSchema {
    asMap = Collections.unmodifiableMap(new LinkedHashMap<>(asMap));
  }

  public static JsonSchema empty() {
    return new JsonSchema(Map.of());
  }

  public static JsonSchema stringType() {
    return new JsonSchema(Map.of(TYPE_KEYWORD, STRING_TYPE));
  }

  public static JsonSchema objectType() {
    return new JsonSchema(Map.of(TYPE_KEYWORD, OBJECT_TYPE));
  }

  public static JsonSchema objectOf(Map<String, JsonSchema> properties, List<String> required) {
    Map<String, Object> propertySchemas = new LinkedHashMap<>();
    properties.forEach((name, schema) -> propertySchemas.put(name, schema.asMap()));

    Map<String, Object> objectSchema = new LinkedHashMap<>();
    objectSchema.put(TYPE_KEYWORD, OBJECT_TYPE);
    objectSchema.put(PROPERTIES_KEYWORD, propertySchemas);
    objectSchema.put(REQUIRED_KEYWORD, List.copyOf(required));
    return new JsonSchema(objectSchema);
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
