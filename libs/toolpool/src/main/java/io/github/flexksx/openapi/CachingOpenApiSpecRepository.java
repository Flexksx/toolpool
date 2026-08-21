package io.github.flexksx.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class CachingOpenApiSpecRepository implements OpenApiSpecRepository {

  private final Map<String, CachedOpenApiSpec> cacheBySpecLocation = new ConcurrentHashMap<>();
  private final OpenApiSpecReader specReader;
  private final Duration refreshInterval;
  private final Clock clock;

  public CachingOpenApiSpecRepository(
      OpenApiSpecReader specReader, Duration refreshInterval, Clock clock) {
    this.specReader = Objects.requireNonNull(specReader, "specReader");
    this.refreshInterval = Objects.requireNonNull(refreshInterval, "refreshInterval");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public OpenAPI get(String specLocation) throws OpenApiSpecReadException {
    try {
      return cacheBySpecLocation.compute(specLocation, this::refreshIfExpired).openAPI();
    } catch (UncheckedSpecReadFailure carriedFailure) {
      throw carriedFailure.readFailure;
    }
  }

  private CachedOpenApiSpec refreshIfExpired(String specLocation, CachedOpenApiSpec cached) {
    if (cached != null && !cached.isExpired(refreshInterval, clock.instant())) {
      return cached;
    }
    try {
      return new CachedOpenApiSpec(specReader.read(specLocation), clock.instant());
    } catch (OpenApiSpecReadException readFailure) {
      if (cached == null) {
        throw new UncheckedSpecReadFailure(readFailure);
      }
      return cached;
    }
  }

  private static final class UncheckedSpecReadFailure extends RuntimeException {

    private final transient OpenApiSpecReadException readFailure;

    private UncheckedSpecReadFailure(OpenApiSpecReadException readFailure) {
      super(readFailure);
      this.readFailure = readFailure;
    }
  }
}
