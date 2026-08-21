package io.github.flexksx.tools;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Optional;

public record ToolParameter(
    String name,
    ParameterLocation location,
    boolean required,
    Schema<?> schema,
    String description) {

  static Optional<ToolParameter> of(Parameter parameter) {
    return ParameterLocation.of(parameter.getIn())
        .map(
            location ->
                new ToolParameter(
                    parameter.getName(),
                    location,
                    Boolean.TRUE.equals(parameter.getRequired()),
                    parameter.getSchema(),
                    parameter.getDescription()));
  }
}
