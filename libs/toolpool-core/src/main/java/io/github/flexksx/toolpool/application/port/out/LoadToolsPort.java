package io.github.flexksx.toolpool.application.port.out;

import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.List;

public interface LoadToolsPort {
  List<Tool> loadTools() throws ToolCatalogUnavailableException;
}
