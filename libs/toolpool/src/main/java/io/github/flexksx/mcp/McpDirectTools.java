package io.github.flexksx.mcp;

import io.github.flexksx.openapi.OpenApiSpecReadException;
import io.github.flexksx.openapi.OpenApiSpecRepository;
import io.github.flexksx.tools.HttpRoute;
import io.github.flexksx.tools.RouteCaller;
import io.github.flexksx.tools.RouteTable;
import io.github.flexksx.tools.ToolInputSchema;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class McpDirectTools {

  private static final Pattern UNSUPPORTED_NAME_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_-]");
  private static final int MAX_TOOL_NAME_LENGTH = 64;
  private static final String DESCRIPTION_SEPARATOR = "\n";

  private final OpenApiSpecRepository specRepository;
  private final String specLocation;
  private final RouteCaller routeCaller;

  public McpDirectTools(
      OpenApiSpecRepository specRepository, String specLocation, RouteCaller routeCaller) {
    this.specRepository = Objects.requireNonNull(specRepository, "specRepository");
    this.specLocation = Objects.requireNonNull(specLocation, "specLocation");
    this.routeCaller = Objects.requireNonNull(routeCaller, "routeCaller");
  }

  public List<SyncToolSpecification> toolSpecifications() throws OpenApiSpecReadException {
    return RouteTable.of(specRepository.get(specLocation)).routes().stream()
        .map(this::toolSpecification)
        .toList();
  }

  private SyncToolSpecification toolSpecification(HttpRoute route) {
    Tool tool =
        Tool.builder(mcpToolName(route.toolName()), ToolInputSchema.of(route))
            .title(route.operation().getSummary())
            .description(description(route))
            .build();
    return SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> routeCaller.call(route, request.arguments()))
        .build();
  }

  private static String description(HttpRoute route) {
    Operation operation = route.operation();
    String description =
        Stream.of(operation.getSummary(), operation.getDescription())
            .filter(text -> text != null && !text.isBlank())
            .collect(Collectors.joining(DESCRIPTION_SEPARATOR));
    return description.isBlank() ? route.method() + " " + route.path() : description;
  }

  private static String mcpToolName(String operationId) {
    String sanitized = UNSUPPORTED_NAME_CHARACTERS.matcher(operationId).replaceAll("_");
    return sanitized.length() <= MAX_TOOL_NAME_LENGTH
        ? sanitized
        : sanitized.substring(0, MAX_TOOL_NAME_LENGTH);
  }
}
