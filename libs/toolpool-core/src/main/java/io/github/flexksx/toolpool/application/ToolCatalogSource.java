package io.github.flexksx.toolpool.application;

import io.github.flexksx.toolpool.domain.tool.ToolCatalog;

public interface ToolCatalogSource {
  ToolCatalog catalog() throws ToolCatalogUnavailableException;
}
