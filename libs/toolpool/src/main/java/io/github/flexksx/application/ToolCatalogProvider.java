package io.github.flexksx.application;

import io.github.flexksx.domain.tool.ToolCatalog;

public interface ToolCatalogProvider {
  ToolCatalog catalog() throws ToolCatalogUnavailableException;
}
