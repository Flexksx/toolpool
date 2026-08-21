package io.github.flexksx.tools;

import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ToolInputSchema {

  public static final String BODY_PROPERTY = "body";

  private static final Logger LOGGER = LoggerFactory.getLogger(ToolInputSchema.class);
  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final String OBJECT_TYPE = "object";

  private ToolInputSchema() {}

  public static Map<String, Object> of(HttpRoute route) {
    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> requiredProperties = new ArrayList<>();

    for (ToolParameter parameter : route.parameters()) {
      if (properties.containsKey(parameter.name())) {
        LOGGER.warn(
            "Ignored the duplicate parameter name {} on {} {}",
            parameter.name(),
            route.method(),
            route.path());
        continue;
      }
      properties.put(parameter.name(), propertySchema(parameter.schema(), parameter.description()));
      if (parameter.required()) {
        requiredProperties.add(parameter.name());
      }
    }

    RequestBody requestBody = route.operation().getRequestBody();
    Schema<?> bodySchema = jsonBodySchema(route, requestBody);
    if (bodySchema != null) {
      properties.put(BODY_PROPERTY, propertySchema(bodySchema, requestBody.getDescription()));
      if (Boolean.TRUE.equals(requestBody.getRequired())) {
        requiredProperties.add(BODY_PROPERTY);
      }
    }

    Map<String, Object> inputSchema = new LinkedHashMap<>();
    inputSchema.put("type", OBJECT_TYPE);
    inputSchema.put("properties", properties);
    inputSchema.put("required", requiredProperties);
    return inputSchema;
  }

  private static Map<String, Object> propertySchema(Schema<?> schema, String description) {
    Map<String, Object> propertySchema =
        schema == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(Json31.jsonSchemaAsMap(schema));
    if (description != null && !description.isBlank()) {
      propertySchema.putIfAbsent("description", description);
    }
    return propertySchema;
  }

  private static Schema<?> jsonBodySchema(HttpRoute route, RequestBody requestBody) {
    if (requestBody == null || requestBody.getContent() == null) {
      return null;
    }
    MediaType jsonContent = requestBody.getContent().get(JSON_CONTENT_TYPE);
    if (jsonContent == null) {
      LOGGER.warn(
          "Ignored the request body of {} {} because it declares no {} content",
          route.method(),
          route.path(),
          JSON_CONTENT_TYPE);
      return null;
    }
    return jsonContent.getSchema();
  }
}
