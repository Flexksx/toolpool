package io.github.flexksx.toolpool.adapter.mcp.metatool;

import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.List;
import java.util.Map;

final class SearchMcpMetatool extends McpMetatool {

  private static final String NAME = "tool_search";
  private static final String DESCRIPTION =
      "Search the callable tools by name, summary, description or tag";

  private static final String QUERY_ARGUMENT = "query";

  private static final JsonSchema INPUT_SCHEMA =
      JsonSchema.objectOf(
          Map.of(
              QUERY_ARGUMENT,
              JsonSchema.stringType()
                  .withDescription("Text to match. Leave empty to list every tool.")),
          List.of());

  SearchMcpMetatool(Toolpool toolpool) {
    super(toolpool);
  }

  @Override
  String name() {
    return NAME;
  }

  @Override
  String description() {
    return DESCRIPTION;
  }

  @Override
  JsonSchema inputSchema() {
    return INPUT_SCHEMA;
  }

  @Override
  CallToolResult execute(Map<String, Object> arguments) throws ToolCatalogUnavailableException {
    return jsonResult(toolpool().search(queryOf(arguments)).stream().map(ToolSummary::of).toList());
  }

  private static String queryOf(Map<String, Object> arguments) {
    Object query = arguments.get(QUERY_ARGUMENT);
    return query == null ? "" : query.toString();
  }
}
