import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.mcp.McpDirectTools;
import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import io.github.flexksx.tools.RouteCaller;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

public class McpDirectToolsTest {

  private static final String SAMPLE_SPEC = "openapi-specs/sample-rest-api-client.openapi.json";

  private final McpDirectTools directTools =
      new McpDirectTools(
          new SwaggerOpenApiSpecReader()::read,
          SAMPLE_SPEC,
          new RouteCaller(RestClient.builder().baseUrl("http://api.test").build()));

  @Test
  void toolSpecifications_registersOneMcpToolPerOperationIdWithADescribedInputSchema()
      throws Exception {
    var specifications = directTools.toolSpecifications();

    assertThat(specifications)
        .extracting(SyncToolSpecification::tool)
        .extracting(Tool::name)
        .containsExactlyInAnyOrder("getUser", "createUser", "updateUser");
    assertThat(specifications)
        .extracting(SyncToolSpecification::tool)
        .allSatisfy(
            tool -> {
              assertThat(tool.description()).isNotBlank();
              assertThat(tool.inputSchema()).containsEntry("type", "object");
            });
  }

  @Test
  void toolSpecifications_sanitizesUnsupportedCharactersInOperationId() throws Exception {
    var tools =
        new McpDirectTools(loc -> specWithOpId("get /users/{id}"), "test", defaultRouteCaller());

    assertThat(tools.toolSpecifications())
        .extracting(s -> s.tool().name())
        .containsExactly("get__users__id_");
  }

  @Test
  void toolSpecifications_usesMethodPathAsFallbackDescription() throws Exception {
    var tools = new McpDirectTools(loc -> specWithOpId("test"), "test", defaultRouteCaller());

    assertThat(tools.toolSpecifications())
        .extracting(s -> s.tool().description())
        .containsExactly("GET /test");
  }

  private static RouteCaller defaultRouteCaller() {
    return new RouteCaller(RestClient.builder().baseUrl("http://api.test").build());
  }

  private static OpenAPI specWithOpId(String operationId) {
    var spec = new OpenAPI();
    var paths = new Paths();
    var pathItem = new PathItem();
    var operation = new Operation();
    operation.setOperationId(operationId);
    pathItem.setGet(operation);
    paths.addPathItem("/test", pathItem);
    spec.setPaths(paths);
    return spec;
  }
}
