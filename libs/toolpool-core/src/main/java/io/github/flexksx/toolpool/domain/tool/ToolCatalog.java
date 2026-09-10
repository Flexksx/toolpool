package io.github.flexksx.toolpool.domain.tool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class ToolCatalog {

  private final Map<ToolName, Tool> toolsByName;

  private ToolCatalog(Map<ToolName, Tool> toolsByName) {
    this.toolsByName = toolsByName;
  }

  public static ToolCatalog of(Collection<Tool> tools) {
    Map<ToolName, Tool> toolsByName = new LinkedHashMap<>();
    for (Tool tool : tools) {
      Tool clashing = toolsByName.putIfAbsent(tool.name(), tool);
      if (clashing != null) {
        throw new DuplicateToolNameException(tool.name(), clashing.target(), tool.target());
      }
    }
    return new ToolCatalog(toolsByName);
  }

  public Tool find(ToolName name) throws UnknownToolException {
    Tool tool = toolsByName.get(name);
    if (tool == null) {
      throw new UnknownToolException(name);
    }
    return tool;
  }

  public List<Tool> search(@Nullable String query) {
    return toolsByName.values().stream().filter(tool -> tool.matches(query)).toList();
  }

  public List<Tool> tools() {
    return List.copyOf(toolsByName.values());
  }
}
