package io.github.flexksx.toolpool.adapter.mcp.translation;

import io.github.flexksx.toolpool.adapter.mcp.DuplicatePublishedNameException;
import io.github.flexksx.toolpool.adapter.mcp.McpToolCallMapper;
import io.github.flexksx.toolpool.adapter.mcp.McpToolNames;
import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class DirectMcpTranslation implements McpTranslation {

  private final Toolpool toolpool;

  public DirectMcpTranslation(Toolpool toolpool) {
    this.toolpool = toolpool;
  }

  @Override
  public List<SyncToolSpecification> toolSpecifications() throws ToolCatalogUnavailableException {
    Map<String, Tool> toolsByPublishedName = new LinkedHashMap<>();
    List<SyncToolSpecification> specifications = new ArrayList<>();

    for (Tool tool : toolpool.tools()) {
      String publishedName = McpToolNames.publishedNameOf(tool.name());
      Tool clashing = toolsByPublishedName.putIfAbsent(publishedName, tool);
      if (clashing != null) {
        throw new DuplicatePublishedNameException(publishedName, clashing.name(), tool.name());
      }
      specifications.add(toolSpecification(publishedName, tool));
    }

    return List.copyOf(specifications);
  }

  private SyncToolSpecification toolSpecification(String publishedName, Tool tool) {
    McpSchema.Tool mcpTool =
        McpSchema.Tool.builder(publishedName, tool.inputSchema().asMap())
            .title(tool.documentation().summary())
            .description(tool.description())
            .build();
    return SyncToolSpecification.builder()
        .tool(mcpTool)
        .callHandler((_, request) -> call(tool.name(), request.arguments()))
        .build();
  }

  private McpSchema.CallToolResult call(ToolName name, @Nullable Map<String, Object> arguments) {
    try {
      return McpToolCallMapper.toCallToolResult(toolpool.call(name, arguments));
    } catch (ToolCatalogUnavailableException | UnknownToolException | RuntimeException failure) {
      return McpToolCallMapper.toFailedCallToolResult(failure);
    }
  }
}
