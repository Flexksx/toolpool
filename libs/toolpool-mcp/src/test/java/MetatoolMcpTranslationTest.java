import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.adapter.mcp.metatool.McpMetatool;
import io.github.flexksx.toolpool.adapter.mcp.translation.MetatoolMcpTranslation;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.github.flexksx.toolpool.testing.ToolpoolFixtures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MetatoolMcpTranslationTest {

  private static final String UNKNOWN_TOOL_NAME = "noSuchTool";
  private static final ToolCallResult BACKEND_ANSWER = new ToolCallResult("{\"id\":\"u1\"}", false);

  private final ToolpoolFixtures.RecordingCallExecutor callExecutor =
      new ToolpoolFixtures.RecordingCallExecutor(BACKEND_ANSWER);
  private final MetatoolMcpTranslation translation =
      new MetatoolMcpTranslation(
          McpMetatool.specificationsOf(
              ToolpoolFixtures.toolpoolFor(ToolpoolFixtures.SAMPLE_SPEC, callExecutor)));

  @Test
  void toolSpecifications_exposesOnlyTheThreeMetaTools() throws Exception {
    assertThat(translation.toolSpecifications())
        .extracting(SyncToolSpecification::tool)
        .extracting(McpSchema.Tool::name)
        .containsExactly("tool_search", "read_tool", "tool_call");
  }

  @Test
  void toolSpecifications_declaresTheRequiredArgumentOfEachMetaTool() throws Exception {
    assertThat(translation.toolSpecifications())
        .extracting(specification -> specification.tool().inputSchema().get("required"))
        .containsExactly(List.of(), List.of("toolName"), List.of("toolName", "arguments"));
  }

  @Test
  void readTool_answersTheDescriptionAndTheInputSchemaOfTheTool() throws Exception {
    CallToolResult result = call("read_tool", Map.of("toolName", "updateUser"));

    assertThat(result.isError()).isFalse();
    assertThat(textOf(result))
        .contains("\"name\":\"updateUser\"")
        .contains("\"inputSchema\"")
        .contains("\"required\":[\"id\",\"body\"]");
  }

  @Test
  void readToolByAnUnknownName_answersAnErrorNamingTheTool() throws Exception {
    CallToolResult result = call("read_tool", Map.of("toolName", UNKNOWN_TOOL_NAME));

    assertThat(result.isError()).isTrue();
    assertThat(textOf(result)).contains(UNKNOWN_TOOL_NAME);
  }

  @Test
  void readToolWithoutAToolName_answersAnErrorNamingTheArgument() throws Exception {
    CallToolResult result = call("read_tool", Map.of());

    assertThat(result.isError()).isTrue();
    assertThat(textOf(result)).contains("toolName");
  }

  @Test
  void toolSearchWithoutAQuery_summarisesEveryTool() throws Exception {
    assertThat(textOf(call("tool_search", Map.of("query", ""))))
        .contains("getUser")
        .contains("createUser")
        .contains("updateUser");
  }

  @Test
  void toolSearchWithAQuery_keepsOnlyTheMatchingTools() throws Exception {
    String matches = textOf(call("tool_search", Map.of("query", "create")));

    assertThat(matches).contains("createUser").doesNotContain("updateUser");
    assertThat(matches).contains("\"summary\":\"Create a user\"").contains("\"tags\":[\"User\"]");
  }

  @Test
  void toolCall_bindsEveryArgumentAndAnswersTheBackendResult() throws Exception {
    CallToolResult result =
        call(
            "tool_call",
            Map.of(
                "toolName",
                "getUser",
                "arguments",
                Map.of("id", "u1", "verbose", true, "X-Request-Id", "r1")));

    assertThat(result.isError()).isFalse();
    assertThat(callExecutor.onlyCall().pathVariables()).containsExactly(Map.entry("id", "u1"));
    assertThat(callExecutor.onlyCall().queryParameters())
        .containsExactly(Map.entry("verbose", List.of("true")));
    assertThat(callExecutor.onlyCall().headers()).containsExactly(Map.entry("X-Request-Id", "r1"));
  }

  @Test
  void toolCallByAnUnknownName_answersAnErrorNamingTheTool() throws Exception {
    CallToolResult result =
        call("tool_call", Map.of("toolName", UNKNOWN_TOOL_NAME, "arguments", Map.of()));

    assertThat(result.isError()).isTrue();
    assertThat(textOf(result)).contains(UNKNOWN_TOOL_NAME);
    assertThat(callExecutor.calls()).isEmpty();
  }

  @Test
  void toolCallWithoutAnArgumentsObject_answersAnErrorNamingTheArgument() throws Exception {
    CallToolResult result = call("tool_call", Map.of("toolName", "getUser"));

    assertThat(result.isError()).isTrue();
    assertThat(textOf(result)).contains("arguments");
    assertThat(callExecutor.calls()).isEmpty();
  }

  private CallToolResult call(String metaToolName, Map<String, Object> arguments) throws Exception {
    SyncToolSpecification specification =
        translation.toolSpecifications().stream()
            .filter(candidate -> candidate.tool().name().equals(metaToolName))
            .findFirst()
            .orElseThrow();
    return specification
        .callHandler()
        .apply(null, new McpSchema.CallToolRequest(metaToolName, arguments, null));
  }

  private static String textOf(CallToolResult result) {
    return ((McpSchema.TextContent) result.content().getFirst()).text();
  }
}
