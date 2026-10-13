package io.github.flexksx.toolpool.application.service;

import io.github.flexksx.toolpool.application.port.in.SearchToolsUseCase;
import io.github.flexksx.toolpool.application.port.out.SearchToolsPort;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class SearchToolsService implements SearchToolsUseCase {

  private final SearchToolsPort searchToolsPort;

  public SearchToolsService(SearchToolsPort searchToolsPort) {
    this.searchToolsPort = searchToolsPort;
  }

  @Override
  public List<Tool> searchTools(@Nullable String query) throws ToolCatalogUnavailableException {
    return searchToolsPort.searchTools(query);
  }
}
