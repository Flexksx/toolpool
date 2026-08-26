import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.adapter.openapi.OpenApiToolCatalogProvider;
import io.github.flexksx.adapter.openapi.OpenApiToolCatalogTranslator;
import io.github.flexksx.domain.http.HttpMethod;
import io.github.flexksx.domain.http.HttpTarget;
import io.github.flexksx.domain.http.ParameterLocation;
import io.github.flexksx.domain.tool.DuplicateToolNameException;
import io.github.flexksx.domain.tool.Tool;
import io.github.flexksx.domain.tool.ToolBody;
import io.github.flexksx.domain.tool.ToolCatalog;
import io.github.flexksx.domain.tool.ToolName;
import io.github.flexksx.domain.tool.ToolParameter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

public class OpenApiToolCatalogTranslatorTest {

  private static final String DUPLICATE_SPEC = "openapi-specs/duplicate-operation-id.openapi.json";
  private static final String MISSING_ID_SPEC = "openapi-specs/missing-operation-id.openapi.json";
  private static final String COOKIE_SPEC = "openapi-specs/cookie-parameter.openapi.json";

  @Test
  void translateASpec_buildsOneToolPerOperationKeyedByItsOperationId() throws Exception {
    ToolCatalog catalog = translate(ToolpoolFixtures.SAMPLE_SPEC);

    assertThat(catalog.tools())
        .extracting(tool -> tool.name().value())
        .containsExactlyInAnyOrder("getUser", "createUser", "updateUser");
    assertThat(catalog.find(ToolName.of("updateUser")).target())
        .isEqualTo(new HttpTarget(HttpMethod.PUT, "/users/{id}"));
  }

  @Test
  void translateAnOperation_mapsEveryParameterToItsLocationAndRequiredFlag() throws Exception {
    Tool getUser = translate(ToolpoolFixtures.SAMPLE_SPEC).find(ToolName.of("getUser"));

    assertThat(getUser.parameters())
        .extracting(ToolParameter::name, ToolParameter::location, ToolParameter::required)
        .containsExactly(
            Tuple.tuple("id", ParameterLocation.PATH, true),
            Tuple.tuple("verbose", ParameterLocation.QUERY, false),
            Tuple.tuple("X-Request-Id", ParameterLocation.HEADER, false));
  }

  @Test
  void translateAnOperationWithAJsonBody_carriesTheFullyResolvedBodySchema() throws Exception {
    Tool updateUser = translate(ToolpoolFixtures.SAMPLE_SPEC).find(ToolName.of("updateUser"));

    ToolBody body = updateUser.body();
    assertThat(body).isNotNull();
    assertThat(body.required()).isTrue();
    assertThat(body.schema().asMap())
        .containsEntry("type", "object")
        .extractingByKey("properties")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsOnlyKeys("id", "name");
  }

  @Test
  void translateAnOperationWithoutAnOperationId_skipsThatOperation() throws Exception {
    assertThat(translate(MISSING_ID_SPEC).tools())
        .extracting(tool -> tool.name().value())
        .containsExactly("listUsers");
  }

  @Test
  void translateAnOperationWithAnUnsupportedParameterLocation_skipsThatParameterOnly()
      throws Exception {
    Tool listUsers = translate(COOKIE_SPEC).find(ToolName.of("listUsers"));

    assertThat(listUsers.parameters()).extracting(ToolParameter::name).containsExactly("page");
  }

  @Test
  void translateAnOperationThatDeclaresOneNameTwice_keepsTheFirstDeclarationOnly() {
    Operation operation = new Operation().operationId("getUser");
    operation.addParametersItem(new PathParameter().name("id").schema(new StringSchema()));
    operation.addParametersItem(new QueryParameter().name("id").schema(new StringSchema()));

    Tool getUser =
        OpenApiToolCatalogTranslator.translate(specWith("/users/{id}", operation))
            .tools()
            .getFirst();

    assertThat(getUser.parameters())
        .extracting(ToolParameter::name, ToolParameter::location)
        .containsExactly(Tuple.tuple("id", ParameterLocation.PATH));
  }

  @Test
  void translateARequestBodyWithoutJsonContent_leavesTheToolWithoutABody() throws Exception {
    assertThat(translate(COOKIE_SPEC).find(ToolName.of("createUser")).body()).isNull();
  }

  @Test
  void translateASpecWithADuplicateOperationId_throwsNamingBothTargets() {
    assertThatThrownBy(() -> translate(DUPLICATE_SPEC))
        .isInstanceOf(DuplicateToolNameException.class)
        .hasMessageContainingAll("listUsers", "/users", "/people");
  }

  @Test
  void translateAnEmptySpec_buildsAnEmptyCatalog() {
    assertThat(OpenApiToolCatalogTranslator.translate(new OpenAPI()).tools()).isEmpty();
  }

  private static OpenAPI specWith(String path, Operation operation) {
    PathItem pathItem = new PathItem();
    pathItem.setGet(operation);
    Paths paths = new Paths();
    paths.addPathItem(path, pathItem);
    return new OpenAPI().paths(paths);
  }

  private static ToolCatalog translate(String specLocation) throws Exception {
    return new OpenApiToolCatalogProvider(specLocation).catalog();
  }
}
