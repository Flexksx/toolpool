package io.github.flexksx.toolpool.spring.embedded;

import io.github.flexksx.toolpool.adapter.openapi.OpenApiToolCatalogSource;
import io.github.flexksx.toolpool.application.port.out.FindToolPort;
import io.github.flexksx.toolpool.application.port.out.LoadToolsPort;
import io.github.flexksx.toolpool.application.port.out.SearchToolsPort;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

final class EmbeddedToolCatalogSource implements LoadToolsPort, FindToolPort, SearchToolsPort {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedToolCatalogSource.class);

  private final LocalServerAddress address;
  private final RequestMappingHandlerMapping handlerMapping;
  private final String apiDocsPath;

  private @Nullable ToolCatalog catalog;

  EmbeddedToolCatalogSource(
      LocalServerAddress address, RequestMappingHandlerMapping handlerMapping, String apiDocsPath) {
    this.address = address;
    this.handlerMapping = handlerMapping;
    this.apiDocsPath = apiDocsPath;
  }

  @Override
  public List<Tool> loadTools() throws ToolCatalogUnavailableException {
    return loadToolCatalog().tools();
  }

  @Override
  public Tool findTool(ToolName name) throws ToolCatalogUnavailableException, UnknownToolException {
    return loadToolCatalog().find(name);
  }

  @Override
  public List<Tool> searchTools(@Nullable String query) throws ToolCatalogUnavailableException {
    return loadToolCatalog().search(query);
  }

  private synchronized ToolCatalog loadToolCatalog() throws ToolCatalogUnavailableException {
    if (catalog == null) {
      Set<HttpTarget> targets = ToolpoolToolScanner.targetsOf(handlerMapping.getHandlerMethods());
      String specLocation = address.baseUrl() + apiDocsPath;
      catalog = new OpenApiToolCatalogSource(specLocation).loadToolCatalog().restrictedTo(targets);
      warnAboutTargetsWithoutAnOperation(targets, catalog, specLocation);
    }
    return catalog;
  }

  private static void warnAboutTargetsWithoutAnOperation(
      Set<HttpTarget> targets, ToolCatalog catalog, String specLocation) {
    List<HttpTarget> exposedTargets = catalog.tools().stream().map(Tool::target).toList();
    targets.stream()
        .filter(target -> !exposedTargets.contains(target))
        .forEach(
            target ->
                LOGGER
                    .atWarn()
                    .setMessage("Toolpool finds no operation for the @ToolpoolTool {} in {}")
                    .addArgument(target.describe())
                    .addArgument(specLocation)
                    .log());
  }
}
