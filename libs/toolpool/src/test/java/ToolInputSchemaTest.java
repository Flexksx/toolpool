import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import io.github.flexksx.tools.RouteTable;
import io.github.flexksx.tools.ToolInputSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ToolInputSchemaTest {

  private static final String SAMPLE_SPEC = "openapi-specs/sample-rest-api-client.openapi.json";

  private final RouteTable routeTable = sampleRouteTable();

  @Test
  void ofARouteWithParameters_keysOnePropertyPerParameterAndRequiresOnlyTheRequiredOnes()
      throws Exception {
    Map<String, Object> inputSchema = ToolInputSchema.of(routeTable.route("getUser"));

    assertThat(inputSchema).containsEntry("type", "object");
    assertThat(propertiesOf(inputSchema)).containsOnlyKeys("id", "verbose", "X-Request-Id");
    assertThat(inputSchema.get("required")).isEqualTo(java.util.List.of("id"));
    assertThat(propertyOf(inputSchema, "verbose")).containsEntry("type", "boolean");
  }

  @Test
  void ofARouteWithARequestBody_nestsTheFullyResolvedBodyUnderOneProperty() throws Exception {
    Map<String, Object> inputSchema = ToolInputSchema.of(routeTable.route("updateUser"));

    assertThat(propertiesOf(inputSchema)).containsOnlyKeys("id", ToolInputSchema.BODY_PROPERTY);
    assertThat(inputSchema.get("required"))
        .isEqualTo(java.util.List.of("id", ToolInputSchema.BODY_PROPERTY));
    assertThat(propertyOf(inputSchema, ToolInputSchema.BODY_PROPERTY))
        .containsEntry("type", "object")
        .extractingByKey("properties")
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
        .containsOnlyKeys("id", "name");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertiesOf(Map<String, Object> inputSchema) {
    return (Map<String, Object>) inputSchema.get("properties");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertyOf(Map<String, Object> inputSchema, String name) {
    return (Map<String, Object>) propertiesOf(inputSchema).get(name);
  }

  private static RouteTable sampleRouteTable() {
    try {
      return RouteTable.of(new SwaggerOpenApiSpecReader().read(SAMPLE_SPEC));
    } catch (Exception readFailure) {
      throw new IllegalStateException(readFailure);
    }
  }
}
