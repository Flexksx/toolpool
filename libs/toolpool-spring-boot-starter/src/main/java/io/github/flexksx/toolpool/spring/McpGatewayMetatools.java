package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.adapter.mcp.McpCallToolResults;
import io.github.flexksx.toolpool.adapter.mcp.ToolDefinition;
import io.github.flexksx.toolpool.adapter.mcp.ToolSummary;
import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

public class McpGatewayMetatools {

  private final Toolpool toolpool;

  public McpGatewayMetatools(Toolpool toolpool) {
    this.toolpool = toolpool;
  }

  @McpTool(
      name = "tool_search",
      description = "Search the callable tools by name, summary, description or tag")
  public List<ToolSummary> toolSearch(
      @McpToolParam(
              description = "Text to match. Leave empty to list every tool.",
              required = false)
          @Nullable String query)
      throws ToolCatalogUnavailableException {
    return toolpool.search(query).stream().map(ToolSummary::of).toList();
  }

  @McpTool(
      name = "read_tool",
      description = "Read the description and the input schema of one tool")
  public ToolDefinition readTool(
      @McpToolParam(description = "The name of the tool to read") String toolName)
      throws ToolCatalogUnavailableException, UnknownToolException {
    return ToolDefinition.of(toolpool.read(ToolName.of(toolName)));
  }

  @McpTool(name = "tool_call", description = "Call a tool by name, passing its input arguments")
  public CallToolResult toolCall(
      @McpToolParam(description = "The name of the tool to call") String toolName,
      @McpToolParam(description = "JSON object holding one entry per tool parameter")
          Map<String, Object> arguments)
      throws ToolCatalogUnavailableException, UnknownToolException {
    return McpCallToolResults.of(toolpool.call(ToolName.of(toolName), arguments));
  }
}
