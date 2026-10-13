package io.github.flexksx.toolpool.adapter.mcp.metatool;

import io.github.flexksx.toolpool.application.port.in.DescribeToolUseCase;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

final class ReadMcpMetatool extends McpMetatool {

  private static final String NAME = "read_tool";
  private static final String DESCRIPTION = "Read the description and the input schema of one tool";
  private static final JsonSchema INPUT_SCHEMA =
      JsonSchema.objectOf(
          Map.of(
              TOOL_NAME_ARGUMENT,
              JsonSchema.stringType().withDescription("The name of the tool to read")),
          List.of(TOOL_NAME_ARGUMENT));

  private final DescribeToolUseCase describeToolUseCase;

  ReadMcpMetatool(DescribeToolUseCase describeToolUseCase) {
    this.describeToolUseCase = describeToolUseCase;
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
  CallToolResult execute(Map<String, Object> arguments, @Nullable AccessToken callerToken)
      throws ToolCatalogUnavailableException, UnknownToolException {
    return jsonResult(
        ToolDefinition.of(describeToolUseCase.describeTool(requiredToolName(arguments))));
  }
}
