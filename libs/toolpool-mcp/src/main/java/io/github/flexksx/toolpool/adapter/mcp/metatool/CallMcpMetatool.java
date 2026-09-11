package io.github.flexksx.toolpool.adapter.mcp.metatool;

import io.github.flexksx.toolpool.adapter.mcp.McpToolCallMapper;
import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CallMcpMetatool extends McpMetatool {

  private static final String NAME = "tool_call";
  private static final String DESCRIPTION = "Call a tool by name, passing its input arguments";

  private static final String ARGUMENTS_ARGUMENT = "arguments";

  private static final JsonSchema INPUT_SCHEMA =
      JsonSchema.objectOf(
          Map.of(
              TOOL_NAME_ARGUMENT,
              JsonSchema.stringType().withDescription("The name of the tool to call"),
              ARGUMENTS_ARGUMENT,
              JsonSchema.objectType()
                  .withDescription("JSON object holding one entry per tool parameter")),
          List.of(TOOL_NAME_ARGUMENT, ARGUMENTS_ARGUMENT));

  private final McpToolCallMapper callMapper = new McpToolCallMapper();

  CallMcpMetatool(Toolpool toolpool) {
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
  CallToolResult execute(Map<String, Object> arguments)
      throws ToolCatalogUnavailableException, UnknownToolException {
    return callMapper.from(
        toolpool().call(requiredToolName(arguments), callArgumentsOf(arguments)));
  }

  private static Map<String, Object> callArgumentsOf(Map<String, Object> arguments) {
    if (!(arguments.get(ARGUMENTS_ARGUMENT) instanceof Map<?, ?> nested)) {
      throw new IllegalArgumentException(
          "Argument " + ARGUMENTS_ARGUMENT + " must be a JSON object");
    }
    Map<String, Object> bound = new LinkedHashMap<>();
    nested.forEach((name, value) -> bound.put(String.valueOf(name), value));
    return bound;
  }
}
