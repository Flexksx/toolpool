package io.github.flexksx.openapi;

import org.springframework.beans.factory.annotation.Value;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

public class SwaggerSpecRepository implements SpecRepository {

  private static final OpenAPIV3Parser PARSER = new OpenAPIV3Parser();
  private final String specLocation;
  private volatile OpenAPI cachedOpenApiSpec;

  public SwaggerSpecRepository(String spechPath) {
    if (spechPath == null || spechPath.isBlank()) {
      throw new IllegalArgumentException("Spec location cannot be null or blank.");
    }
    this.specLocation = spechPath;
  }

  @Override
  public OpenAPI getOpenApi() {
    OpenAPI result = cachedOpenApiSpec;
    if (result == null) {
      synchronized (this) {
        result = cachedOpenApiSpec;
        if (result == null) {
          result = PARSER.read(specLocation);
          if (result == null) {
            throw new IllegalStateException(
                "Failed to parse OpenAPI spec at location: " + specLocation);
          }
          cachedOpenApiSpec = result;
        }
      }
    }
    return result;
  }
}
