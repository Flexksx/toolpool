package io.github.flexksx.toolpool.adapter.openapi;

import io.github.flexksx.toolpool.domain.http.HttpMethod;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import io.github.flexksx.toolpool.domain.tool.ToolDocumentation;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenApiToolCatalogMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiToolCatalogMapper.class);
  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final Set<ParameterLocation> DECLARABLE_LOCATIONS =
      EnumSet.of(ParameterLocation.PATH, ParameterLocation.QUERY, ParameterLocation.HEADER);

  private OpenApiToolCatalogMapper() {}

  public static ToolCatalog toToolCatalog(OpenAPI spec) {
    List<Tool> tools = new ArrayList<>();
    Paths paths = spec.getPaths();
    if (paths != null) {
      paths.forEach(
          (path, pathItem) ->
              pathItem
                  .readOperationsMap()
                  .forEach(
                      (method, operation) ->
                          toTool(path, method, operation).ifPresent(tools::add)));
    }
    return ToolCatalog.of(tools);
  }

  private static Optional<Tool> toTool(
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
            new ToolName(operationId),
            target,
            toDocumentation(operation),
            toParameters(target, operation)));
  }

  private static ToolDocumentation toDocumentation(Operation operation) {
    List<String> tags = operation.getTags();
    return new ToolDocumentation(
        operation.getSummary(), operation.getDescription(), tags == null ? List.of() : tags);
  }

  private static List<ToolParameter> toParameters(HttpTarget target, Operation operation) {
    List<ToolParameter> toolParameters = new ArrayList<>();
    Set<String> seenNames = new HashSet<>();
    if (operation.getParameters() != null) {
      for (Parameter parameter : operation.getParameters()) {
        toDeclaredParameter(target, parameter, seenNames).ifPresent(toolParameters::add);
      }
    }
    toBody(target, operation.getRequestBody())
        .ifPresent(
            body -> {
              if (seenNames.add(body.name())) {
                toolParameters.add(body);
              } else {
                LOGGER.warn(
                    "Ignored the request body of {} because a parameter already claims the name {}",
                    target.describe(),
                    body.name());
              }
            });
    return List.copyOf(toolParameters);
  }

  private static Optional<ToolParameter> toDeclaredParameter(
      HttpTarget target, Parameter parameter, Set<String> seenNames) {
    Optional<ParameterLocation> location = toParameterLocation(parameter.getIn());
    if (location.isEmpty()) {
      LOGGER.warn(
          "Skipped the parameter {} of {} because the location {} is not supported",
          parameter.getName(),
          target.describe(),
          parameter.getIn());
      return Optional.empty();
    }
    if (!seenNames.add(parameter.getName())) {
      LOGGER.warn(
          "Skipped the parameter {} of {} because that name is declared more than once",
          parameter.getName(),
          target.describe());
      return Optional.empty();
    }
    return Optional.of(
        new ToolParameter(
            parameter.getName(),
            location.get(),
            Boolean.TRUE.equals(parameter.getRequired()),
            toJsonSchema(parameter.getSchema()),
            parameter.getDescription()));
  }

  private static Optional<ParameterLocation> toParameterLocation(@Nullable String openApiLocation) {
    return DECLARABLE_LOCATIONS.stream()
        .filter(location -> location.name().equalsIgnoreCase(openApiLocation))
        .findFirst();
  }

  private static Optional<ToolParameter> toBody(
      HttpTarget target, @Nullable RequestBody requestBody) {
    if (requestBody == null || requestBody.getContent() == null) {
      return Optional.empty();
    }
    MediaType jsonContent = requestBody.getContent().get(JSON_CONTENT_TYPE);
    if (jsonContent == null) {
      LOGGER.warn(
          "Ignored the request body of {} because it declares no {} content",
          target.describe(),
          JSON_CONTENT_TYPE);
      return Optional.empty();
    }
    return Optional.of(
        ToolParameter.body(
            Boolean.TRUE.equals(requestBody.getRequired()),
            toJsonSchema(jsonContent.getSchema()),
            requestBody.getDescription()));
  }

  private static JsonSchema toJsonSchema(@Nullable Schema<?> schema) {
    return schema == null ? JsonSchema.empty() : new JsonSchema(Json31.jsonSchemaAsMap(schema));
  }
}
