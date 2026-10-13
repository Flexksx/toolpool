package io.github.flexksx.toolpool.adapter.openapi;

import io.github.flexksx.toolpool.application.port.out.FindToolPort;
import io.github.flexksx.toolpool.application.port.out.LoadToolsPort;
import io.github.flexksx.toolpool.application.port.out.SearchToolsPort;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class OpenApiToolCatalogSource implements LoadToolsPort, FindToolPort, SearchToolsPort {

  private static final ParseOptions PARSE_OPTIONS = fullyResolvingParseOptions();

  private final String specLocation;

  private @Nullable ToolCatalog catalog;

  public OpenApiToolCatalogSource(String specLocation) {
    this.specLocation = specLocation;
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

  public synchronized ToolCatalog loadToolCatalog() throws ToolCatalogUnavailableException {
    if (catalog == null) {
      catalog = OpenApiToolCatalogMapper.toToolCatalog(specAtSpecLocation());
    }
    return catalog;
  }

  private OpenAPI specAtSpecLocation() throws ToolCatalogUnavailableException {
    SwaggerParseResult result;
    try {
      result = new OpenAPIV3Parser().readLocation(specLocation, null, PARSE_OPTIONS);
    } catch (RuntimeException readFailure) {
      throw new ToolCatalogUnavailableException(specLocation, readFailure);
    }
    if (result == null || result.getOpenAPI() == null) {
      throw new ToolCatalogUnavailableException(specLocation);
    }
    return result.getOpenAPI();
  }

  private static ParseOptions fullyResolvingParseOptions() {
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setResolve(true);
    parseOptions.setResolveFully(true);
    return parseOptions;
  }
}
