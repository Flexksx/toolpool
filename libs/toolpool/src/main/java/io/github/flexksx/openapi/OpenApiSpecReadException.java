package io.github.flexksx.openapi;

public class OpenApiSpecReadException extends Exception {

  public OpenApiSpecReadException(String specLocation) {
    super("Could not read an OpenAPI spec at " + specLocation);
  }

  public OpenApiSpecReadException(String specLocation, Throwable cause) {
    super("Could not read an OpenAPI spec at " + specLocation, cause);
  }
}
