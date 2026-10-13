package io.github.flexksx.toolpool.application.port.in;

import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.List;

public interface ListToolsUseCase {
  List<Tool> listTools() throws ToolCatalogUnavailableException;
}
