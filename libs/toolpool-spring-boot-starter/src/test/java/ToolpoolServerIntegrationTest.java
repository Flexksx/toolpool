import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

@SpringBootTest(
    classes = EmbeddedTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "toolpool.embedded.enabled=true",
      "toolpool.mode=direct",
      "toolpool.server.port=0"
    })
class ToolpoolServerIntegrationTest {

  @LocalServerPort private int applicationPort;

  @Value("${local.toolpool.port}")
  private int toolpoolPort;

  @Test
  void listToolsOnTheToolpoolPort_exposesOnlyTheMarkedEndpoints() {
    try (McpSyncClient client = initializedClient(toolpoolPort)) {
      assertThat(client.listTools().tools())
          .extracting(McpSchema.Tool::name)
          .containsExactlyInAnyOrder("getUser", "listOrders");
    }
  }

  @Test
  void callAMarkedEndpointOnTheToolpoolPort_answersFromTheMainApplication() {
    try (McpSyncClient client = initializedClient(toolpoolPort)) {
      CallToolResult result =
          client.callTool(CallToolRequest.builder("getUser").arguments(Map.of("id", "u1")).build());

      assertThat(result.isError()).isFalse();
      assertThat(((TextContent) result.content().getFirst()).text()).contains("\"u1\"");
    }
  }

  @Test
  void initializeOnTheApplicationPort_findsNoMcpEndpoint() throws Exception {
    assertThat(AuthTestMcpClient.initializeWith(applicationPort, null).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void getAnApplicationRouteOnTheToolpoolPort_findsNoRoute() throws Exception {
    assertThat(AuthTestMcpClient.get(toolpoolPort, "/users/u1").statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  private static McpSyncClient initializedClient(int port) {
    McpSyncClient client =
        McpClient.sync(
                HttpClientStreamableHttpTransport.builder(AuthTestMcpClient.baseUrlOf(port))
                    .endpoint(AuthTestMcpClient.MCP_ENDPOINT)
                    .build())
            .build();
    client.initialize();
    return client;
  }
}
