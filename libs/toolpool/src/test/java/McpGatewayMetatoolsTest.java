import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.mcp.McpGatewayMetatools;
import io.github.flexksx.openapi.CachingOpenApiSpecRepository;
import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import io.swagger.v3.oas.models.Operation;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class McpGatewayMetatoolsTest {

  private static final String SPEC_LOCATION = "openapi-specs/sample-rest-api-client.openapi.json";
  private static final String TOOL_NAME_UPDATE_USER = "updateUser";
  private static final String TOOL_NAME_CREATE_USER = "createUser";
  private static final String UNKNOWN_TOOL_NAME = "noSuchTool";
  private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);

  private final McpGatewayMetatools metatools =
      new McpGatewayMetatools(
          new CachingOpenApiSpecRepository(
              new SwaggerOpenApiSpecReader(), REFRESH_INTERVAL, Clock.systemUTC()),
          SPEC_LOCATION);

  @Test
  void readToolByOperationId_returnsThatOperation() throws Exception {
    assertThat(metatools.readTool(TOOL_NAME_UPDATE_USER))
        .isInstanceOfSatisfying(
            Operation.class,
            operation -> assertThat(operation.getOperationId()).isEqualTo(TOOL_NAME_UPDATE_USER));
  }

  @Test
  void readToolByAnotherOperationId_returnsThatOtherOperation() throws Exception {
    assertThat(metatools.readTool(TOOL_NAME_CREATE_USER))
        .isInstanceOfSatisfying(
            Operation.class,
            operation -> assertThat(operation.getOperationId()).isEqualTo(TOOL_NAME_CREATE_USER));
  }

  @Test
  void readToolByAnUnknownName_returnsNull() throws Exception {
    assertThat(metatools.readTool(UNKNOWN_TOOL_NAME)).isNull();
  }
}
