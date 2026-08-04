import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.flexksx.mcp.McpGatewayMetatools;
import io.github.flexksx.openapi.SpecRepository;
import io.github.flexksx.openapi.SwaggerSpecRepository;

public class McpLaunchTest {
  private static final String DEFAULT_DEMO_SPEC_RESOURCE =
      "./openapi-specs/sample-rest-api-client.openapi.json";
  private static final String TOOL_NAME_UPDATE_USER = "updateUser";
  private McpGatewayMetatools mcpGatewayMetatools;
  private SpecRepository specRepository;

  @BeforeEach
  void setup() {
    specRepository = new SwaggerSpecRepository(DEFAULT_DEMO_SPEC_RESOURCE, Duration.ofMinutes(1));
    mcpGatewayMetatools = new McpGatewayMetatools(specRepository);
  }

  @Test
  void loadSpec() {
    assertNotNull(specRepository.getOpenApi());
  }

  @Test
  void mcpReadToolByOperationId() {
    Object toolResponse = mcpGatewayMetatools.readTool(TOOL_NAME_UPDATE_USER);
    assertNotNull(toolResponse);
    System.out.println(toolResponse);
  }
}
