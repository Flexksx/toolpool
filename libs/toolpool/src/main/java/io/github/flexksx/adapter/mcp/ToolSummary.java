package io.github.flexksx.adapter.mcp;

import io.github.flexksx.domain.tool.Tool;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record ToolSummary(String name, @Nullable String summary, List<String> tags) {

  public static ToolSummary of(Tool tool) {
    return new ToolSummary(
        tool.name().value(), tool.documentation().summary(), tool.documentation().tags());
  }
}
