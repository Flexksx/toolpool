package io.github.flexksx.tools;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RouteTable {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteTable.class);

  private final Map<String, HttpRoute> routesByToolName;

  private RouteTable(Map<String, HttpRoute> routesByToolName) {
    this.routesByToolName = routesByToolName;
  }

  public static RouteTable of(OpenAPI spec) {
    Objects.requireNonNull(spec, "spec");
    Map<String, HttpRoute> routesByToolName = new LinkedHashMap<>();
    Paths paths = spec.getPaths();
    if (paths != null) {
      paths.forEach(
          (path, pathItem) ->
              pathItem
                  .readOperationsMap()
                  .forEach(
                      (method, operation) ->
                          add(routesByToolName, HttpRoute.of(path, method, operation))));
    }
    return new RouteTable(routesByToolName);
  }

  public HttpRoute route(String toolName) throws UnknownToolException {
    HttpRoute route = routesByToolName.get(toolName);
    if (route == null) {
      throw new UnknownToolException(toolName);
    }
    return route;
  }

  public List<HttpRoute> search(String query) {
    if (query == null || query.isBlank()) {
      return routes();
    }
    String lowerCasedQuery = query.toLowerCase(Locale.ROOT);
    return routesByToolName.values().stream()
        .filter(route -> searchableText(route).contains(lowerCasedQuery))
        .toList();
  }

  public List<HttpRoute> routes() {
    return List.copyOf(routesByToolName.values());
  }

  private static String searchableText(HttpRoute route) {
    Operation operation = route.operation();
    return Stream.of(
            route.toolName(),
            operation.getSummary(),
            operation.getDescription(),
            operation.getTags() == null ? null : String.join(" ", operation.getTags()))
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" "))
        .toLowerCase(Locale.ROOT);
  }

  private static void add(Map<String, HttpRoute> routesByToolName, HttpRoute candidate) {
    String toolName = candidate.toolName();
    if (toolName == null || toolName.isBlank()) {
      LOGGER.warn(
          "Skipped {} {} because the OpenAPI operation declares no operationId",
          candidate.method(),
          candidate.path());
      return;
    }
    HttpRoute clashing = routesByToolName.putIfAbsent(toolName, candidate);
    if (clashing != null) {
      throw new IllegalStateException(
          "Duplicate operationId "
              + toolName
              + " on "
              + clashing.method()
              + " "
              + clashing.path()
              + " and "
              + candidate.method()
              + " "
              + candidate.path());
    }
  }
}
