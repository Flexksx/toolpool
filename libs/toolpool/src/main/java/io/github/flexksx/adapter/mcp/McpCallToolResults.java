package io.github.flexksx.adapter.mcp;

import io.github.flexksx.domain.tool.ToolCallResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

final class McpCallToolResults {

  private McpCallToolResults() {}

  static CallToolResult of(ToolCallResult result) {
    return CallToolResult.builder()
        .addTextContent(result.content())
        .isError(result.failed())
        .build();
  }

  static CallToolResult errorOf(Exception failure) {
    return CallToolResult.builder()
        .addTextContent(failure.getMessage() == null ? failure.toString() : failure.getMessage())
        .isError(true)
        .build();
  }
}
