import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import io.github.flexksx.tools.HttpRoute;
import io.github.flexksx.tools.RouteCaller;
import io.github.flexksx.tools.RouteTable;
import io.github.flexksx.tools.ToolInputSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class RouteCallerTest {

  private static final String SAMPLE_SPEC = "openapi-specs/sample-rest-api-client.openapi.json";
  private static final String BASE_URL = "http://api.test";
  private static final String USER_JSON = "{\"id\":\"u1\",\"name\":\"Ana\"}";

  private RouteTable routeTable;
  private MockRestServiceServer apiServer;
  private RouteCaller routeCaller;

  @BeforeEach
  void bindCallerToAMockedApi() throws Exception {
    routeTable = RouteTable.of(new SwaggerOpenApiSpecReader().read(SAMPLE_SPEC));
    RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
    apiServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    routeCaller = new RouteCaller(restClientBuilder.build());
  }

  @Test
  void callARouteWithEveryParameterLocation_sendsEachArgumentToItsOwnLocation() throws Exception {
    apiServer
        .expect(requestTo(BASE_URL + "/users/u1?verbose=true"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Request-Id", "r1"))
        .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));

    CallToolResult result =
        routeCaller.call(
            routeTable.route("getUser"), Map.of("id", "u1", "verbose", true, "X-Request-Id", "r1"));

    apiServer.verify();
    assertThat(result.isError()).isFalse();
    assertThat(textOf(result)).isEqualTo(USER_JSON);
  }

  @Test
  void callARouteWithARequestBody_sendsTheBodyPropertyAsJson() throws Exception {
    apiServer
        .expect(requestTo(BASE_URL + "/users/u1"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(USER_JSON))
        .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));

    CallToolResult result =
        routeCaller.call(
            routeTable.route("updateUser"),
            Map.of("id", "u1", ToolInputSchema.BODY_PROPERTY, orderedUserBody()));

    apiServer.verify();
    assertThat(result.isError()).isFalse();
    assertThat(textOf(result)).isEqualTo(USER_JSON);
  }

  @Test
  void callARouteThatAnswersAnErrorStatus_returnsTheBodyMarkedAsAnError() throws Exception {
    apiServer
        .expect(requestTo(BASE_URL + "/users/missing"))
        .andRespond(
            withStatus(HttpStatus.NOT_FOUND).body("{\"detail\":\"User missing not found\"}"));

    CallToolResult result = routeCaller.call(routeTable.route("getUser"), Map.of("id", "missing"));

    assertThat(result.isError()).isTrue();
    assertThat(textOf(result)).contains("User missing not found");
  }

  @Test
  void callARouteWithoutARequiredParameter_throwsNamingTheParameter() throws Exception {
    HttpRoute route = routeTable.route("getUser");

    assertThatThrownBy(() -> routeCaller.call(route, Map.of("verbose", true)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContainingAll("path", "id", "getUser");
  }

  @Test
  void callARouteWithoutARequiredBody_throwsIllegalArgument() throws Exception {
    HttpRoute route = routeTable.route("updateUser");

    assertThatThrownBy(() -> routeCaller.call(route, Map.of("id", "u1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContainingAll("body", "updateUser");
  }

  private static Map<String, Object> orderedUserBody() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", "u1");
    body.put("name", "Ana");
    return body;
  }

  private static String textOf(CallToolResult result) {
    return ((TextContent) result.content().getFirst()).text();
  }
}
