package com.flexksx.toolpooldemo;

import static com.flexksx.toolpooldemo.McpServerTestClient.call;
import static com.flexksx.toolpooldemo.McpServerTestClient.textOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.samplerestapiclient.user.UserController;
import io.github.flexksx.samplerestapiclient.user.UserRestExceptionHandler;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
      "server.port=18099",
      "toolpool.mode=metatools",
      "toolpool.base-url=http://localhost:18099"
    })
@Import({UserController.class, UserRestExceptionHandler.class})
class McpMetatoolsServerIntegrationTest {

  private static final int SERVER_PORT = 18099;

  private McpSyncClient mcpClient;

  @BeforeEach
  void openMcpClient() {
    mcpClient = McpServerTestClient.openAgainst(SERVER_PORT);
  }

  @AfterEach
  void closeMcpClient() {
    mcpClient.close();
  }

  @Test
  void listTools_exposesOnlyTheThreeMetatools() {
    assertThat(mcpClient.listTools().tools())
        .extracting(Tool::name)
        .containsExactlyInAnyOrder("tool_search", "read_tool", "tool_call");
  }

  @Test
  void toolSearch_summarisesTheMatchingOperation() {
    CallToolResult result = call(mcpClient, "tool_search", Map.of("query", "update"));

    assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
    assertThat(textOf(result)).contains("updateUser").doesNotContain("createUser");
  }

  @Test
  void readTool_describesTheParametersOfTheOperation() {
    CallToolResult result = call(mcpClient, "read_tool", Map.of("toolName", "getUser"));

    assertThat(textOf(result)).contains("getUser").contains("X-Request-Id").contains("inputSchema");
  }

  @Test
  void toolCall_proxiesEveryArgumentToTheBackingEndpoint() {
    CallToolResult result =
        call(
            mcpClient,
            "tool_call",
            Map.of(
                "toolName",
                "getUser",
                "arguments",
                Map.of("id", "u1", "verbose", true, "X-Request-Id", "r1")));

    assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
    assertThat(textOf(result)).contains("\"id\":\"u1\"").contains("(verbose)");
  }

  @Test
  void toolCall_marksABackendErrorStatusAsAnError() {
    CallToolResult result =
        call(
            mcpClient,
            "tool_call",
            Map.of(
                "toolName", "getUser", "arguments", Map.of("id", UserController.MISSING_USER_ID)));

    assertThat(result.isError()).isTrue();
    assertThat(textOf(result))
        .contains("\"status\":404")
        .contains("/users/" + UserController.MISSING_USER_ID);
  }
}
