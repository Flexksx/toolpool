package io.github.flexksx.toolpool.application;

import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class Toolpool {

  private final ToolCatalogSource catalogSource;
  private final ToolCallExecutor callExecutor;
  private final UpstreamAccessTokenResolver tokenResolver;

  public Toolpool(ToolCatalogSource catalogSource, ToolCallExecutor callExecutor) {
    this(catalogSource, callExecutor, UpstreamAccessTokenResolver.none());
  }

  public Toolpool(
      ToolCatalogSource catalogSource,
      ToolCallExecutor callExecutor,
      UpstreamAccessTokenResolver tokenResolver) {
    this.catalogSource = catalogSource;
    this.callExecutor = callExecutor;
    this.tokenResolver = tokenResolver;
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

  public ToolCallResult call(
      ToolName name, @Nullable Map<String, Object> arguments, @Nullable AccessToken callerToken)
      throws ToolCatalogUnavailableException, UnknownToolException {
    return callExecutor.execute(
        catalogSource
            .catalog()
            .find(name)
            .requestFor(arguments)
            .withAccessToken(tokenResolver.upstreamTokenFor(callerToken)));
  }
}
