package io.github.flexksx.toolpool.adapter.mcp.metatool;

import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.Map;

record ToolDefinition(String name, String description, Map<String, Object> inputSchema) {

  public static ToolDefinition of(Tool tool) {
    return new ToolDefinition(tool.name().value(), tool.description(), tool.inputSchema().asMap());
  }
}
