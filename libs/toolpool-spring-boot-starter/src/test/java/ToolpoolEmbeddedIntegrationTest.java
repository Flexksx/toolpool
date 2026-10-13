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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    classes = EmbeddedTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"toolpool.embedded.enabled=true", "toolpool.mode=direct"})
class ToolpoolEmbeddedIntegrationTest {

  @LocalServerPort private int port;

  @Test
  void listToolsInDirectMode_exposesOnlyTheMarkedEndpoints() {
    try (McpSyncClient client = initializedClient()) {
      assertThat(client.listTools().tools())
          .extracting(McpSchema.Tool::name)
          .containsExactlyInAnyOrder("getUser", "listOrders");
    }
  }

  @Test
  void callAMarkedEndpointInDirectMode_answersFromTheController() {
    try (McpSyncClient client = initializedClient()) {
      CallToolResult result =
          client.callTool(CallToolRequest.builder("getUser").arguments(Map.of("id", "u1")).build());

      assertThat(result.isError()).isFalse();
      assertThat(((TextContent) result.content().getFirst()).text()).contains("\"u1\"");
    }
  }

  private McpSyncClient initializedClient() {
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
