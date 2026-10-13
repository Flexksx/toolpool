package io.github.flexksx.toolpool.adapter.mcp.translation;

import io.github.flexksx.toolpool.adapter.mcp.DuplicatePublishedNameException;
import io.github.flexksx.toolpool.adapter.mcp.McpAccessTokenMapper;
import io.github.flexksx.toolpool.adapter.mcp.McpToolCallMapper;
import io.github.flexksx.toolpool.adapter.mcp.McpToolNames;
import io.github.flexksx.toolpool.application.port.in.CallToolUseCase;
import io.github.flexksx.toolpool.application.port.in.ListToolsUseCase;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
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

  private final ListToolsUseCase listToolsUseCase;
  private final CallToolUseCase callToolUseCase;

  public DirectMcpTranslation(ListToolsUseCase listToolsUseCase, CallToolUseCase callToolUseCase) {
    this.listToolsUseCase = listToolsUseCase;
    this.callToolUseCase = callToolUseCase;
  }

  @Override
  public List<SyncToolSpecification> toolSpecifications() throws ToolCatalogUnavailableException {
    Map<String, Tool> toolsByPublishedName = new LinkedHashMap<>();
    List<SyncToolSpecification> specifications = new ArrayList<>();

    for (Tool tool : listToolsUseCase.listTools()) {
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
        .callHandler(
            (exchange, request) ->
                call(
                    tool.name(), request.arguments(), McpAccessTokenMapper.toCallerToken(exchange)))
        .build();
  }

  private McpSchema.CallToolResult call(
      ToolName name, @Nullable Map<String, Object> arguments, @Nullable AccessToken callerToken) {
    try {
      return McpToolCallMapper.toCallToolResult(
          callToolUseCase.callTool(name, arguments, callerToken));
    } catch (ToolCatalogUnavailableException | UnknownToolException | RuntimeException failure) {
      return McpToolCallMapper.toFailedCallToolResult(failure);
    }
  }
}
