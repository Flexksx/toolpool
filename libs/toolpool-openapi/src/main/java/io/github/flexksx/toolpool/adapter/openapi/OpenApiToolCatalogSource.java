package io.github.flexksx.toolpool.adapter.openapi;

import io.github.flexksx.toolpool.application.ToolCatalogSource;
import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.jspecify.annotations.Nullable;

public class OpenApiToolCatalogSource implements ToolCatalogSource {

  private static final ParseOptions PARSE_OPTIONS = fullyResolvingParseOptions();

  private final String specLocation;

  private @Nullable ToolCatalog catalog;

  public OpenApiToolCatalogSource(String specLocation) {
    this.specLocation = specLocation;
  }

  @Override
  public synchronized ToolCatalog catalog() throws ToolCatalogUnavailableException {
    if (catalog == null) {
      catalog = OpenApiToolCatalogMapper.map(specAtSpecLocation());
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
