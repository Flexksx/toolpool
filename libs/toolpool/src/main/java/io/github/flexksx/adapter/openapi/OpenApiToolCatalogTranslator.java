package io.github.flexksx.adapter.openapi;

import io.github.flexksx.domain.http.HttpMethod;
import io.github.flexksx.domain.http.HttpTarget;
import io.github.flexksx.domain.http.ParameterLocation;
import io.github.flexksx.domain.schema.JsonSchema;
import io.github.flexksx.domain.tool.Tool;
import io.github.flexksx.domain.tool.ToolBody;
import io.github.flexksx.domain.tool.ToolCatalog;
import io.github.flexksx.domain.tool.ToolDocumentation;
import io.github.flexksx.domain.tool.ToolName;
import io.github.flexksx.domain.tool.ToolParameter;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenApiToolCatalogTranslator {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiToolCatalogTranslator.class);
  private static final String JSON_CONTENT_TYPE = "application/json";

  private OpenApiToolCatalogTranslator() {}

  public static ToolCatalog translate(OpenAPI spec) {
    List<Tool> tools = new ArrayList<>();
    Paths paths = spec.getPaths();
    if (paths != null) {
      paths.forEach(
          (path, pathItem) ->
              pathItem
                  .readOperationsMap()
                  .forEach(
                      (method, operation) ->
                          toolOf(path, method, operation).ifPresent(tools::add)));
    }
    return ToolCatalog.of(tools);
  }

  private static Optional<Tool> toolOf(
      String path, PathItem.HttpMethod method, Operation operation) {
    HttpTarget target = new HttpTarget(HttpMethod.valueOf(method.name()), path);
    String operationId = operation.getOperationId();
    if (operationId == null || operationId.isBlank()) {
      LOGGER.warn(
          "Skipped {} because the OpenAPI operation declares no operationId", target.describe());
      return Optional.empty();
    }
    return Optional.of(
        new Tool(
            ToolName.of(operationId),
            target,
            documentationOf(operation),
            parametersOf(target, operation),
            bodyOf(target, operation.getRequestBody())));
  }

  private static ToolDocumentation documentationOf(Operation operation) {
    List<String> tags = operation.getTags();
    return new ToolDocumentation(
        operation.getSummary(), operation.getDescription(), tags == null ? List.of() : tags);
  }

  private static List<ToolParameter> parametersOf(HttpTarget target, Operation operation) {
    if (operation.getParameters() == null) {
      return List.of();
    }
    List<ToolParameter> toolParameters = new ArrayList<>();
    Set<String> seenNames = new HashSet<>();
    for (Parameter parameter : operation.getParameters()) {
      Optional<ParameterLocation> location = parameterLocationOf(parameter.getIn());
      if (location.isEmpty()) {
        LOGGER.warn(
            "Skipped the parameter {} of {} because the location {} is not supported",
            parameter.getName(),
            target.describe(),
            parameter.getIn());
        continue;
      }
      if (!seenNames.add(parameter.getName())) {
        LOGGER.warn(
            "Skipped the parameter {} of {} because that name is declared more than once",
            parameter.getName(),
            target.describe());
        continue;
      }
      toolParameters.add(
          new ToolParameter(
              parameter.getName(),
              location.get(),
              Boolean.TRUE.equals(parameter.getRequired()),
              jsonSchemaOf(parameter.getSchema()),
              parameter.getDescription()));
    }
    return List.copyOf(toolParameters);
  }

  private static Optional<ParameterLocation> parameterLocationOf(@Nullable String openApiLocation) {
    return Arrays.stream(ParameterLocation.values())
        .filter(location -> location.name().equalsIgnoreCase(openApiLocation))
        .findFirst();
  }

  private static @Nullable ToolBody bodyOf(HttpTarget target, @Nullable RequestBody requestBody) {
    if (requestBody == null || requestBody.getContent() == null) {
      return null;
    }
    MediaType jsonContent = requestBody.getContent().get(JSON_CONTENT_TYPE);
    if (jsonContent == null) {
      LOGGER.warn(
          "Ignored the request body of {} because it declares no {} content",
          target.describe(),
          JSON_CONTENT_TYPE);
      return null;
    }
    return new ToolBody(
        Boolean.TRUE.equals(requestBody.getRequired()),
        jsonSchemaOf(jsonContent.getSchema()),
        requestBody.getDescription());
  }

  private static JsonSchema jsonSchemaOf(@Nullable Schema<?> schema) {
    return schema == null ? JsonSchema.empty() : new JsonSchema(Json31.jsonSchemaAsMap(schema));
  }
}
