package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class McpDirectTools {

  private final Toolpool toolpool;

  public McpDirectTools(Toolpool toolpool) {
    this.toolpool = toolpool;
  }

  public List<SyncToolSpecification> toolSpecifications() throws ToolCatalogUnavailableException {
    return toolpool.tools().stream().map(this::toolSpecification).toList();
  }

  private SyncToolSpecification toolSpecification(Tool tool) {
    McpSchema.Tool mcpTool =
        McpSchema.Tool.builder(tool.name().value(), tool.inputSchema().asMap())
            .title(tool.documentation().summary())
            .description(tool.description())
            .build();
    return SyncToolSpecification.builder()
        .tool(mcpTool)
        .callHandler((exchange, request) -> call(tool.name(), request.arguments()))
        .build();
  }

  private McpSchema.CallToolResult call(ToolName name, @Nullable Map<String, Object> arguments) {
    try {
      return McpCallToolResults.of(toolpool.call(name, arguments));
    } catch (ToolCatalogUnavailableException | UnknownToolException | RuntimeException failure) {
      return McpCallToolResults.errorOf(failure);
    }
  }
}
