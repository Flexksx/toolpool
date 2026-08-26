import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.adapter.mcp.McpGatewayMetatools;
import io.github.flexksx.adapter.mcp.ToolDefinition;
import io.github.flexksx.adapter.mcp.ToolSummary;
import io.github.flexksx.domain.tool.ToolCallResult;
import io.github.flexksx.domain.tool.UnknownToolException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class McpGatewayMetatoolsTest {

  private static final String UPDATE_USER = "updateUser";
  private static final String UNKNOWN_TOOL_NAME = "noSuchTool";
  private static final ToolCallResult BACKEND_ANSWER = new ToolCallResult("{\"id\":\"u1\"}", false);

  private final ToolpoolFixtures.RecordingCallExecutor callExecutor =
      new ToolpoolFixtures.RecordingCallExecutor(BACKEND_ANSWER);
  private final McpGatewayMetatools metatools =
      new McpGatewayMetatools(
          ToolpoolFixtures.toolpoolFor(ToolpoolFixtures.SAMPLE_SPEC, callExecutor));

  @Test
  void readToolByName_returnsItsDescriptionAndItsInputSchema() throws Exception {
    ToolDefinition definition = metatools.readTool(UPDATE_USER);

    assertThat(definition.name()).isEqualTo(UPDATE_USER);
    assertThat(definition.description()).isNotBlank();
    assertThat(definition.inputSchema())
        .containsEntry("type", "object")
        .containsEntry("required", java.util.List.of("id", "body"));
  }

  @Test
  void readToolByAnUnknownName_throwsNamingTheTool() {
    assertThatThrownBy(() -> metatools.readTool(UNKNOWN_TOOL_NAME))
        .isInstanceOf(UnknownToolException.class)
        .hasMessageContaining(UNKNOWN_TOOL_NAME);
  }

  @Test
  void toolSearchWithoutAQuery_summarisesEveryTool() throws Exception {
    assertThat(metatools.toolSearch(""))
        .extracting(ToolSummary::name)
        .containsExactlyInAnyOrder("getUser", "createUser", UPDATE_USER);
  }

  @Test
  void toolSearchWithAQuery_keepsOnlyTheMatchingTools() throws Exception {
    assertThat(metatools.toolSearch("create"))
        .extracting(ToolSummary::name)
        .containsExactly("createUser");
    assertThat(metatools.toolSearch("create"))
        .extracting(ToolSummary::summary, ToolSummary::tags)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Create a user", java.util.List.of("User")));
  }

  @Test
  void toolCall_bindsEveryArgumentAndAnswersTheBackendResult() throws Exception {
    CallToolResult result =
        metatools.toolCall("getUser", Map.of("id", "u1", "verbose", true, "X-Request-Id", "r1"));

    assertThat(result.isError()).isFalse();
    assertThat(callExecutor.onlyCall().pathVariables()).containsExactly(Map.entry("id", "u1"));
    assertThat(callExecutor.onlyCall().queryParameters())
        .containsExactly(Map.entry("verbose", java.util.List.of("true")));
    assertThat(callExecutor.onlyCall().headers()).containsExactly(Map.entry("X-Request-Id", "r1"));
  }

  @Test
  void toolCallByAnUnknownName_throwsNamingTheTool() {
    assertThatThrownBy(() -> metatools.toolCall(UNKNOWN_TOOL_NAME, Map.of()))
        .isInstanceOf(UnknownToolException.class)
        .hasMessageContaining(UNKNOWN_TOOL_NAME);
  }
}
