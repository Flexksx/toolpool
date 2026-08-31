package io.github.flexksx.application;

import io.github.flexksx.domain.tool.Tool;
import io.github.flexksx.domain.tool.ToolCallResult;
import io.github.flexksx.domain.tool.ToolName;
import io.github.flexksx.domain.tool.UnknownToolException;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class Toolpool {

  private final ToolCatalogSource catalogSource;
  private final ToolCallExecutor callExecutor;

  public Toolpool(ToolCatalogSource catalogSource, ToolCallExecutor callExecutor) {
    this.catalogSource = catalogSource;
    this.callExecutor = callExecutor;
  }

  public List<Tool> tools() throws ToolCatalogUnavailableException {
    return catalogSource.catalog().tools();
  }

  public List<Tool> search(@Nullable String query) throws ToolCatalogUnavailableException {
    return catalogSource.catalog().search(query);
  }

  public Tool read(ToolName name) throws ToolCatalogUnavailableException, UnknownToolException {
    return catalogSource.catalog().find(name);
  }

  public ToolCallResult call(ToolName name, @Nullable Map<String, Object> arguments)
      throws ToolCatalogUnavailableException, UnknownToolException {
    return callExecutor.execute(catalogSource.catalog().find(name).bind(arguments));
  }
}
