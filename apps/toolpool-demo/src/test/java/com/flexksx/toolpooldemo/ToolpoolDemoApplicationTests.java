package com.flexksx.toolpooldemo;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.mcp.McpGatewayMetatools;
import io.swagger.v3.oas.models.Operation;
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
        .isInstanceOfSatisfying(
            Operation.class,
            operation -> assertThat(operation.getOperationId()).isEqualTo(TOOL_NAME_UPDATE_USER));
  }
}
