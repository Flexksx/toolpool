package io.github.flexksx.mcp;

import io.github.flexksx.openapi.OpenApiSpecReadException;
import io.github.flexksx.openapi.OpenApiSpecRepository;
import java.util.Map;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class McpGatewayMetatools {

  private final OpenApiSpecRepository specRepository;
  private final String specLocation;

  public McpGatewayMetatools(
      OpenApiSpecRepository specRepository,
      @Value("${toolpool.spec-location}") String specLocation) {
    this.specRepository = specRepository;
    this.specLocation = specLocation;
  }

  @McpTool(name = "call_tool", description = "Call a tool by name, passing input parameters")
  public Object callTool(
      @McpToolParam(description = "The name of the tool to call") String toolName,
      @McpToolParam(description = "JSON object containing input") Map<String, Object> input) {

    return 0;
  }

  @McpTool(name = "read_tool", description = "Read the spec of a callable tool")
  public Object readTool(
      @McpToolParam(description = "The name of the tool to read.") String toolName)
      throws OpenApiSpecReadException {
    return specRepository.get(specLocation).getPaths().values().stream()
        .flatMap(pathItem -> pathItem.readOperations().stream())
        .filter(operation -> toolName.equals(operation.getOperationId()))
        .findFirst()
        .orElse(null);
  }
}
