package io.github.flexksx.application;

import io.github.flexksx.domain.tool.ToolCatalog;

public interface ToolCatalogSource {
  ToolCatalog catalog() throws ToolCatalogUnavailableException;
}
