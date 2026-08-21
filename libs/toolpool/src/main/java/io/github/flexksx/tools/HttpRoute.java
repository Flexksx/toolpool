package io.github.flexksx.tools;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record HttpRoute(
    String path, PathItem.HttpMethod method, Operation operation, List<ToolParameter> parameters) {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpRoute.class);

  static HttpRoute of(String path, PathItem.HttpMethod method, Operation operation) {
    return new HttpRoute(path, method, operation, toolParameters(path, method, operation));
  }

  public String toolName() {
    return operation.getOperationId();
  }

  private static List<ToolParameter> toolParameters(
      String path, PathItem.HttpMethod method, Operation operation) {
    if (operation.getParameters() == null) {
      return List.of();
    }
    List<ToolParameter> toolParameters = new ArrayList<>();
    for (Parameter parameter : operation.getParameters()) {
      ToolParameter.of(parameter)
          .ifPresentOrElse(
              toolParameters::add,
              () ->
                  LOGGER.warn(
                      "Skipped the parameter {} of {} {} because the location {} is not supported",
                      parameter.getName(),
                      method,
                      path,
                      parameter.getIn()));
    }
    return List.copyOf(toolParameters);
  }
}
