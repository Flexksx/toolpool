package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public final class McpToolCallMapper {

  private McpToolCallMapper() {}

  public static CallToolResult toCallToolResult(ToolCallResult result) {
    return CallToolResult.builder()
        .addTextContent(result.content())
        .isError(result.failed())
        .build();
  }

  public static CallToolResult toFailedCallToolResult(Exception failure) {
    return CallToolResult.builder()
        .addTextContent(failure.getMessage() == null ? failure.toString() : failure.getMessage())
        .isError(true)
        .build();
  }
}
