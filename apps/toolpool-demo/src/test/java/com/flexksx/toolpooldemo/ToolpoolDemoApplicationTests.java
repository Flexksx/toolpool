package com.flexksx.toolpooldemo;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.mcp.McpGatewayMetatools;
import io.github.flexksx.tools.ToolSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ToolpoolDemoApplicationTests {

  private static final String TOOL_NAME_UPDATE_USER = "updateUser";

  @Autowired private McpGatewayMetatools mcpGatewayMetatools;

  @Test
  void readToolResolvesAgainstTheConfiguredSpecLocation() throws Exception {
    assertThat(mcpGatewayMetatools.readTool(TOOL_NAME_UPDATE_USER))
        .contains("\"operationId\" : \"" + TOOL_NAME_UPDATE_USER + "\"");
  }

  @Test
  void toolSearchMatchesTheOperationSummary() throws Exception {
    assertThat(mcpGatewayMetatools.toolSearch("update"))
        .extracting(ToolSummary::name)
        .containsExactly(TOOL_NAME_UPDATE_USER);
  }
}
