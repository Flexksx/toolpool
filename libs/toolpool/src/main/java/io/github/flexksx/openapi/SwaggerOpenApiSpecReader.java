package io.github.flexksx.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

public class SwaggerOpenApiSpecReader implements OpenApiSpecReader {

  private final OpenAPIV3Parser parser = new OpenAPIV3Parser();

  @Override
  public OpenAPI read(String specLocation) throws OpenApiSpecReadException {
    validateSpecLocation(specLocation);

    OpenAPI spec;
    try {
      spec = parser.read(specLocation);
    } catch (RuntimeException readFailure) {
      throw new OpenApiSpecReadException(specLocation, readFailure);
    }

    if (spec == null) {
      throw new OpenApiSpecReadException(specLocation);
    }
    return spec;
  }

  private static void validateSpecLocation(String specLocation) {
    if (specLocation == null || specLocation.isBlank()) {
      throw new IllegalArgumentException("Spec location cannot be null or blank.");
    }
  }
}
