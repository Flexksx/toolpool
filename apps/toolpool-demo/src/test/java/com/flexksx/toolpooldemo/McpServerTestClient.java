package com.flexksx.toolpooldemo;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.Map;

final class McpServerTestClient {

  private static final String MCP_ENDPOINT = "/mcp";

  private McpServerTestClient() {}

  static McpSyncClient openAgainst(int serverPort) {
    McpSyncClient client =
        McpClient.sync(
                HttpClientStreamableHttpTransport.builder("http://localhost:" + serverPort)
                    .endpoint(MCP_ENDPOINT)
                    .build())
            .build();
    client.initialize();
    return client;
  }

  static CallToolResult call(McpSyncClient client, String toolName, Map<String, Object> arguments) {
    return client.callTool(CallToolRequest.builder(toolName).arguments(arguments).build());
  }

  static String textOf(CallToolResult result) {
    return ((TextContent) result.content().getFirst()).text();
  }
}
