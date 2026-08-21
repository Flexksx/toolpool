package io.github.flexksx.tools;

import io.swagger.v3.oas.models.Operation;
import java.util.List;

public record ToolSummary(String name, String summary, List<String> tags) {

  public static ToolSummary of(HttpRoute route) {
    Operation operation = route.operation();
    return new ToolSummary(
        route.toolName(),
        operation.getSummary() == null ? operation.getDescription() : operation.getSummary(),
        operation.getTags() == null ? List.of() : List.copyOf(operation.getTags()));
  }
}
