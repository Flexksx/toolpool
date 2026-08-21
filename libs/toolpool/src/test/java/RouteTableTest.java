import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import io.github.flexksx.tools.HttpRoute;
import io.github.flexksx.tools.RouteTable;
import io.github.flexksx.tools.UnknownToolException;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.Test;

public class RouteTableTest {

  private static final String SAMPLE_SPEC = "openapi-specs/sample-rest-api-client.openapi.json";
  private static final String DUPLICATE_SPEC = "openapi-specs/duplicate-operation-id.openapi.json";
  private static final String MISSING_ID_SPEC = "openapi-specs/missing-operation-id.openapi.json";

  private final SwaggerOpenApiSpecReader reader = new SwaggerOpenApiSpecReader();

  @Test
  void ofASpec_keysEveryRouteByItsOperationId() throws Exception {
    RouteTable routeTable = routeTableOf(SAMPLE_SPEC);

    assertThat(routeTable.routes())
        .extracting(HttpRoute::toolName)
        .containsExactlyInAnyOrder("getUser", "createUser", "updateUser");
    assertThat(routeTable.route("updateUser"))
        .returns("/users/{id}", HttpRoute::path)
        .returns(PathItem.HttpMethod.PUT, HttpRoute::method);
  }

  @Test
  void routeForAnUnknownToolName_throwsNamingTheTool() throws Exception {
    RouteTable routeTable = routeTableOf(SAMPLE_SPEC);

    assertThatThrownBy(() -> routeTable.route("deleteUser"))
        .isInstanceOf(UnknownToolException.class)
        .hasMessageContaining("deleteUser");
  }

  @Test
  void ofASpecWithADuplicateOperationId_throwsNamingBothRoutes() throws Exception {
    var spec = reader.read(DUPLICATE_SPEC);

    assertThatThrownBy(() -> RouteTable.of(spec))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContainingAll("listUsers", "/users", "/people");
  }

  @Test
  void ofASpecWithARouteWithoutAnOperationId_skipsThatRoute() throws Exception {
    RouteTable routeTable = routeTableOf(MISSING_ID_SPEC);

    assertThat(routeTable.routes()).extracting(HttpRoute::toolName).containsExactly("listUsers");
  }

  private RouteTable routeTableOf(String specLocation) throws Exception {
    return RouteTable.of(reader.read(specLocation));
  }
}
