package io.github.flexksx.toolpool.application.port.out;

import io.github.flexksx.toolpool.domain.tool.Tool;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface SearchToolsPort {
  List<Tool> searchTools(@Nullable String query) throws ToolCatalogUnavailableException;
}
