package io.github.flexksx.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import java.time.Duration;
import java.time.Instant;

public record CachedOpenApiSpec(OpenAPI openAPI, Instant lastUpdated) {

  boolean isExpired(Duration refreshInterval, Instant now) {
    return Duration.between(lastUpdated, now).compareTo(refreshInterval) > 0;
  }
}
