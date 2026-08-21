import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.mcp.McpGatewayMetatools;
import io.github.flexksx.openapi.CachingOpenApiSpecRepository;
import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import io.github.flexksx.tools.RouteCaller;
import io.github.flexksx.tools.ToolSummary;
import io.github.flexksx.tools.UnknownToolException;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

public class McpGatewayMetatoolsTest {

  private static final String SPEC_LOCATION = "openapi-specs/sample-rest-api-client.openapi.json";
  private static final String TOOL_NAME_UPDATE_USER = "updateUser";
  private static final String UNKNOWN_TOOL_NAME = "noSuchTool";
  private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);

  private final McpGatewayMetatools metatools =
      new McpGatewayMetatools(
          new CachingOpenApiSpecRepository(
              new SwaggerOpenApiSpecReader(), REFRESH_INTERVAL, Clock.systemUTC()),
          SPEC_LOCATION,
          new RouteCaller(RestClient.builder().baseUrl("http://api.test").build()));

  @Test
  void readToolByOperationId_returnsThatOperationAsSelfContainedJson() throws Exception {
    assertThat(metatools.readTool(TOOL_NAME_UPDATE_USER))
        .contains("\"operationId\" : \"" + TOOL_NAME_UPDATE_USER + "\"")
        .contains("\"in\" : \"path\"")
        .doesNotContain("$ref");
  }

  @Test
  void readToolByAnUnknownName_throwsNamingTheTool() {
    assertThatThrownBy(() -> metatools.readTool(UNKNOWN_TOOL_NAME))
        .isInstanceOf(UnknownToolException.class)
        .hasMessageContaining(UNKNOWN_TOOL_NAME);
  }

  @Test
  void toolSearchWithoutAQuery_summarisesEveryTool() throws Exception {
    assertThat(metatools.toolSearch(""))
        .extracting(ToolSummary::name)
        .containsExactlyInAnyOrder("getUser", "createUser", TOOL_NAME_UPDATE_USER);
  }

  @Test
  void toolSearchByTag_keepsOnlyTheMatchingTools() throws Exception {
    assertThat(metatools.toolSearch("payments")).isEmpty();
    assertThat(metatools.toolSearch("user")).hasSize(3);
  }
}
