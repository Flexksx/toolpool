package io.github.flexksx.toolpool.application.service;

import io.github.flexksx.toolpool.application.port.in.ListToolsUseCase;
import io.github.flexksx.toolpool.application.port.out.LoadToolsPort;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.List;

public class ListToolsService implements ListToolsUseCase {

  private final LoadToolsPort loadToolsPort;

  public ListToolsService(LoadToolsPort loadToolsPort) {
    this.loadToolsPort = loadToolsPort;
  }

  @Override
  public List<Tool> listTools() throws ToolCatalogUnavailableException {
    return loadToolsPort.loadTools();
  }
}
