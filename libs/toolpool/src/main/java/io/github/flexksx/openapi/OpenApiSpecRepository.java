package io.github.flexksx.openapi;

import io.swagger.v3.oas.models.OpenAPI;

public interface OpenApiSpecRepository {
  OpenAPI get(String specLocation) throws OpenApiSpecReadException;
}
