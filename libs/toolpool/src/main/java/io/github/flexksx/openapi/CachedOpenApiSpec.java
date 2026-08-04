package io.github.flexksx.openapi;

import java.time.Duration;
import java.time.Instant;

import io.swagger.v3.oas.models.OpenAPI;

public record CachedOpenApiSpec(OpenAPI openAPI, Instant lastUpdated) {
  boolean isExpired(Duration ttl) {
    return Duration.between(lastUpdated, Instant.now()).compareTo(ttl) > 0;
  }
}
