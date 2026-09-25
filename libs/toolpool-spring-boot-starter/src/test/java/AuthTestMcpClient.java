import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

final class AuthTestMcpClient {

  static final String MCP_ENDPOINT = "/mcp";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final String INITIALIZE_REQUEST =
      "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{"
          + "\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
          + "\"clientInfo\":{\"name\":\"test\",\"version\":\"0\"}}}";

  private AuthTestMcpClient() {}

  static String baseUrlOf(int port) {
    return "http://localhost:" + port;
  }

  static Map<String, Object> callGetUser(int port, String bearerToken) {
    return call(port, bearerToken, "getUser", Map.of("id", "u1"));
  }

  static Map<String, Object> callMetatoolGetUser(int port, String bearerToken) {
    return call(
        port,
        bearerToken,
        "tool_call",
        Map.of("toolName", "getUser", "arguments", Map.of("id", "u1")));
  }

  private static Map<String, Object> call(
      int port, String bearerToken, String toolName, Map<String, Object> arguments) {
    try (McpSyncClient client =
        McpClient.sync(
                HttpClientStreamableHttpTransport.builder(baseUrlOf(port))
                    .endpoint(MCP_ENDPOINT)
                    .httpRequestCustomizer(
                        (builder, method, endpoint, body, context) ->
                            builder.header("Authorization", "Bearer " + bearerToken))
                    .build())
            .build()) {
      client.initialize();
      CallToolResult result =
          client.callTool(CallToolRequest.builder(toolName).arguments(arguments).build());
      String text = ((TextContent) result.content().getFirst()).text();
      if (Boolean.TRUE.equals(result.isError())) {
        throw new IllegalStateException("The tool call failed: " + text);
      }
      return JSON.readValue(text, Map.class);
    }
  }

  static HttpResponse<String> initializeWith(int port, String authorization)
      throws IOException, InterruptedException {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(baseUrlOf(port) + MCP_ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST));
    if (authorization != null) {
      request.header("Authorization", authorization);
    }
    return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  static HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
    return HTTP.send(
        HttpRequest.newBuilder(URI.create(baseUrlOf(port) + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
