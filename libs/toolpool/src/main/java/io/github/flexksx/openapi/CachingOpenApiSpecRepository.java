package io.github.flexksx.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
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
    } catch (CompletionException carriedFailure) {
      throw (OpenApiSpecReadException) carriedFailure.getCause();
    }
  }

  private CachedOpenApiSpec refreshIfExpired(String specLocation, CachedOpenApiSpec cached) {
    Instant now = clock.instant();
    if (cached != null && !cached.isExpired(refreshInterval, now)) {
      return cached;
    }
    try {
      return new CachedOpenApiSpec(specReader.read(specLocation), now);
    } catch (OpenApiSpecReadException readFailure) {
      if (cached == null) {
        throw new CompletionException(readFailure);
      }
      return cached;
    }
  }
}
