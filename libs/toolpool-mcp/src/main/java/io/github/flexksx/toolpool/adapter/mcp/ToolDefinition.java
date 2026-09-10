package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.Map;

public record ToolDefinition(String name, String description, Map<String, Object> inputSchema) {

  public static ToolDefinition of(Tool tool) {
    return new ToolDefinition(tool.name().value(), tool.description(), tool.inputSchema().asMap());
  }
}
