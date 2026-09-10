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
      "server.port=18098",
      "toolpool.mode=direct",
      "toolpool.base-url=http://localhost:18098"
    })
@Import({UserController.class, UserRestExceptionHandler.class})
class McpDirectServerIntegrationTest {

  private static final int SERVER_PORT = 18098;

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
  void listTools_exposesOneToolPerOperationIdWithItsInputSchema() {
    var tools = mcpClient.listTools().tools();

    assertThat(tools)
        .extracting(Tool::name)
        .containsExactlyInAnyOrder(
            "getUser", "listUsers", "countUsers", "createUser", "updateUser", "deleteUser");
    assertThat(tools)
        .allSatisfy(
            tool -> {
              assertThat(tool.description()).isNotBlank();
              assertThat(tool.inputSchema()).containsEntry("type", "object");
            });
  }

  @Test
  void callAToolWithARequestBody_sendsTheBodyToTheBackingEndpoint() {
    CallToolResult result =
        call(mcpClient, "updateUser", Map.of("id", "u1", "body", Map.of("name", "Ana")));

    assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
    assertThat(textOf(result)).contains("\"id\":\"u1\"").contains("\"name\":\"Ana\"");
  }
}
