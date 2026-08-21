package io.github.flexksx.mcp;

import io.github.flexksx.openapi.OpenApiSpecReadException;
import io.github.flexksx.openapi.OpenApiSpecRepository;
import io.github.flexksx.tools.RouteCaller;
import io.github.flexksx.tools.RouteTable;
import io.github.flexksx.tools.ToolSummary;
import io.github.flexksx.tools.UnknownToolException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.swagger.v3.core.util.Json31;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

public class McpGatewayMetatools {

  private final OpenApiSpecRepository specRepository;
  private final String specLocation;
  private final RouteCaller routeCaller;

  public McpGatewayMetatools(
      OpenApiSpecRepository specRepository, String specLocation, RouteCaller routeCaller) {
    this.specRepository = Objects.requireNonNull(specRepository, "specRepository");
    this.specLocation = Objects.requireNonNull(specLocation, "specLocation");
    this.routeCaller = Objects.requireNonNull(routeCaller, "routeCaller");
  }

  @McpTool(
      name = "tool_search",
      description = "Search the callable tools by name, summary, description or tag")
  public List<ToolSummary> toolSearch(
      @McpToolParam(
              description = "Text to match. Leave empty to list every tool.",
              required = false)
          String query)
      throws OpenApiSpecReadException {
    return routeTable().search(query).stream().map(ToolSummary::of).toList();
  }

  @McpTool(
      name = "read_tool",
      description = "Read the parameters, request body and responses of one tool")
  public String readTool(
      @McpToolParam(description = "The name of the tool to read") String toolName)
      throws OpenApiSpecReadException, UnknownToolException {
    return Json31.pretty(routeTable().route(toolName).operation());
  }

  @McpTool(name = "tool_call", description = "Call a tool by name, passing its input arguments")
  public CallToolResult toolCall(
      @McpToolParam(description = "The name of the tool to call") String toolName,
      @McpToolParam(description = "JSON object holding one entry per tool parameter")
          Map<String, Object> arguments)
      throws OpenApiSpecReadException, UnknownToolException {
    return routeCaller.call(routeTable().route(toolName), arguments);
  }

  private RouteTable routeTable() throws OpenApiSpecReadException {
    return RouteTable.of(specRepository.get(specLocation));
  }
}
