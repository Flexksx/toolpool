package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public class McpToolCallMapper {

  public CallToolResult from(ToolCallResult result) {
    return CallToolResult.builder()
        .addTextContent(result.content())
        .isError(result.failed())
        .build();
  }

  public CallToolResult fromFailure(Exception failure) {
    return CallToolResult.builder()
        .addTextContent(failure.getMessage() == null ? failure.toString() : failure.getMessage())
        .isError(true)
        .build();
  }
}
