package io.github.flexksx.toolpool.application.port.in;

import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface SearchToolsUseCase {
  List<Tool> searchTools(@Nullable String query) throws ToolCatalogUnavailableException;
}
