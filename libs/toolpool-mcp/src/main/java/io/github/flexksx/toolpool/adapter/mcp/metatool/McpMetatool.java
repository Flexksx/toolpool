package io.github.flexksx.toolpool.adapter.mcp.metatool;

import io.github.flexksx.toolpool.adapter.mcp.McpToolCallMapper;
import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public abstract sealed class McpMetatool
    permits SearchMcpMetatool, ReadMcpMetatool, CallMcpMetatool {

  static final String TOOL_NAME_ARGUMENT = "toolName";

  private final Toolpool toolpool;

  McpMetatool(Toolpool toolpool) {
    this.toolpool = toolpool;
  }

  public static List<SyncToolSpecification> specificationsOf(Toolpool toolpool) {
    return Stream.of(
            new SearchMcpMetatool(toolpool),
            new ReadMcpMetatool(toolpool),
            new CallMcpMetatool(toolpool))
        .map(McpMetatool::specification)
        .toList();
  }

  static ToolName requiredToolName(Map<String, Object> arguments) {
    Object value = arguments.get(TOOL_NAME_ARGUMENT);
    if (value == null || value.toString().isBlank()) {
      throw new IllegalArgumentException("Argument " + TOOL_NAME_ARGUMENT + " is required");
    }
    return new ToolName(value.toString());
  }

  abstract String name();

  abstract String description();

  abstract JsonSchema inputSchema();

  abstract CallToolResult execute(Map<String, Object> arguments)
      throws ToolCatalogUnavailableException, UnknownToolException;

  final Toolpool toolpool() {
    return toolpool;
  }

  final CallToolResult jsonResult(Object payload) {
    try {
      return CallToolResult.builder()
          .addTextContent(McpJsonDefaults.getMapper().writeValueAsString(payload))
          .isError(false)
          .build();
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
  }

  private SyncToolSpecification specification() {
    McpSchema.Tool tool =
        McpSchema.Tool.builder(name(), inputSchema().asMap()).description(description()).build();
    return SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((_, request) -> handleToolCall(request.arguments()))
        .build();
  }

  private CallToolResult handleToolCall(@Nullable Map<String, Object> arguments) {
    try {
      return execute(arguments == null ? Map.of() : arguments);
    } catch (ToolCatalogUnavailableException | UnknownToolException | RuntimeException failure) {
      return McpToolCallMapper.toFailedCallToolResult(failure);
    }
  }
}
