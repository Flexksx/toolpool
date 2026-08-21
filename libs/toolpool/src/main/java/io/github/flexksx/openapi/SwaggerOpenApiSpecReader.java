package io.github.flexksx.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class SwaggerOpenApiSpecReader implements OpenApiSpecReader {

  private static final ParseOptions PARSE_OPTIONS = fullyResolvingParseOptions();

  private final OpenAPIV3Parser parser = new OpenAPIV3Parser();

  @Override
  public OpenAPI read(String specLocation) throws OpenApiSpecReadException {
    validateSpecLocation(specLocation);

    SwaggerParseResult result;
    try {
      result = parser.readLocation(specLocation, null, PARSE_OPTIONS);
    } catch (RuntimeException readFailure) {
      throw new OpenApiSpecReadException(specLocation, readFailure);
    }

    if (result == null || result.getOpenAPI() == null) {
      throw new OpenApiSpecReadException(specLocation);
    }
    return result.getOpenAPI();
  }

  private static ParseOptions fullyResolvingParseOptions() {
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setResolve(true);
    parseOptions.setResolveFully(true);
    return parseOptions;
  }

  private static void validateSpecLocation(String specLocation) {
    if (specLocation == null || specLocation.isBlank()) {
      throw new IllegalArgumentException("Spec location cannot be null or blank.");
    }
  }
}
