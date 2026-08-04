package io.github.flexksx.mcp;

import java.util.List;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import io.github.flexksx.openapi.SpecRepository;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

@Component
public class McpGatewayMetatools {

  private final SpecRepository specRepository;

  public McpGatewayMetatools(SpecRepository specRepository) {
    this.specRepository = specRepository;
  }

  @McpTool(name = "call_tool", description = "Call a tool by name, passing input parameters")
  public Object callTool(
      @McpToolParam(description = "The name of the tool to call") String toolName,
      @McpToolParam(description = "JSON object containing input") Map<String, Object> input) {

    return 0;
  }

  @McpTool(name = "read_tool", description = "Read the spec of a callable tool")
  public Object readTool(
      @McpToolParam(description = "The name of the tool to read.") String toolName) {
    return specRepository.getOpenApi().getPaths().values().stream()
        .flatMap(pathItem -> pathItem.readOperations().stream())
        .filter(operation -> toolName.equals(operation.getOperationId()))
        .findFirst()
        .orElse(null);
  }
}
