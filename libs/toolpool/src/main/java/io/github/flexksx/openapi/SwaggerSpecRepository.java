package io.github.flexksx.openapi;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

public class SwaggerSpecRepository implements SpecRepository {

  private static final OpenAPIV3Parser PARSER = new OpenAPIV3Parser();

  private final AtomicReference<String> specLocation = new AtomicReference<>();
  private final AtomicReference<CachedOpenApiSpec> cachedOpenApiSpec = new AtomicReference<>();
  private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

  private final Duration refreshInterval;

  public SwaggerSpecRepository(String specLocation, Duration refreshInterval) {
    validateSpecLocation(specLocation);
    this.refreshInterval = Objects.requireNonNull(refreshInterval);
    this.specLocation.set(specLocation);
    parseAndCache(specLocation);
  }

  @Override
  public OpenAPI getOpenApi() {
    CachedOpenApiSpec current = cachedOpenApiSpec.get();
    if (current.isExpired(refreshInterval)) {
      refresh();
      current = cachedOpenApiSpec.get();
    }
    return current.openAPI();
  }

  @Override
  public void setSpecLocation(String newLocation) {
    validateSpecLocation(newLocation);

    while (!isRefreshing.compareAndSet(false, true)) {
      Thread.onSpinWait();
    }

    try {
      this.specLocation.set(newLocation);
      parseAndCache(newLocation);
    } finally {
      isRefreshing.set(false);
    }
  }

  private void refresh() {
    if (isRefreshing.compareAndSet(false, true)) {
      try {
        parseAndCache(specLocation.get());
      } catch (Exception _) {
      } finally {
        isRefreshing.set(false);
      }
    }
  }

  private void parseAndCache(String specLocation) {
    OpenAPI spec = null;

    try {
      spec = PARSER.read(specLocation);
    } catch (Exception e) {
      System.err.println(
          "Unexpected error occurred when trying to parse and cache OpenAPI spec: " + e);
    }

    if (spec == null) {
      if (cachedOpenApiSpec.get() != null) {
        return;
      }
      throw new IllegalArgumentException("Could not parse spec at " + specLocation);
    }
    if (specLocation.equals(this.specLocation.get())) {
      cachedOpenApiSpec.set(new CachedOpenApiSpec(spec, Instant.now()));
    }
  }

  private static void validateSpecLocation(String specLocation) {
    if (specLocation == null || specLocation.isBlank()) {
      throw new IllegalArgumentException("Spec location cannot be null or blank.");
    }
  }
}
