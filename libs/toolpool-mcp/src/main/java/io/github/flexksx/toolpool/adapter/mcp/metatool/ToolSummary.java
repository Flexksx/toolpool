package io.github.flexksx.toolpool.adapter.mcp.metatool;

import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.List;
import org.jspecify.annotations.Nullable;

record ToolSummary(String name, @Nullable String summary, List<String> tags) {

  public static ToolSummary of(Tool tool) {
    return new ToolSummary(
        tool.name().value(), tool.documentation().summary(), tool.documentation().tags());
  }
}
