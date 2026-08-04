package io.github.flexksx.openapi;

import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;

@Component
public interface SpecRepository {
  OpenAPI getOpenApi();

  void setSpecLocation(String specLocation);
}
