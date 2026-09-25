import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.adapter.openapi.OpenApiToolCatalogMapper;
import io.github.flexksx.toolpool.adapter.openapi.OpenApiToolCatalogSource;
import io.github.flexksx.toolpool.domain.http.HttpMethod;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.tool.DuplicateToolNameException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import io.github.flexksx.toolpool.testing.ToolpoolFixtures;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.groups.Tuple;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class OpenApiToolCatalogMapperTest {

  private static final String DUPLICATE_SPEC = "openapi-specs/duplicate-operation-id.openapi.json";
  private static final String MISSING_ID_SPEC = "openapi-specs/missing-operation-id.openapi.json";
  private static final String COOKIE_SPEC = "openapi-specs/cookie-parameter.openapi.json";
  private static final String RESERVED_HEADER_SPEC =
      "openapi-specs/reserved-header-parameter.openapi.json";

  @Test
  void toToolCatalogOfASpec_buildsOneToolPerOperationKeyedByItsOperationId() throws Exception {
    ToolCatalog catalog = toToolCatalog(ToolpoolFixtures.SAMPLE_SPEC);

    assertThat(catalog.tools())
        .extracting(tool -> tool.name().value())
        .containsExactlyInAnyOrder("getUser", "createUser", "updateUser");
    assertThat(catalog.find(new ToolName("updateUser")).target())
        .isEqualTo(new HttpTarget(HttpMethod.PUT, "/users/{id}"));
  }

  @Test
  void toToolCatalogOfAnOperation_mapsEveryParameterToItsLocationAndRequiredFlag()
      throws Exception {
    Tool getUser = toToolCatalog(ToolpoolFixtures.SAMPLE_SPEC).find(new ToolName("getUser"));

    assertThat(getUser.parameters())
        .extracting(ToolParameter::name, ToolParameter::location, ToolParameter::required)
        .containsExactly(
            Tuple.tuple("id", ParameterLocation.PATH, true),
            Tuple.tuple("verbose", ParameterLocation.QUERY, false),
            Tuple.tuple("X-Request-Id", ParameterLocation.HEADER, false));
  }

  @Test
  void toToolCatalogOfAnOperationWithAJsonBody_carriesTheFullyResolvedBodySchema()
      throws Exception {
    Tool updateUser = toToolCatalog(ToolpoolFixtures.SAMPLE_SPEC).find(new ToolName("updateUser"));

    ToolParameter body = bodyOf(updateUser);
    assertThat(body).isNotNull();
    assertThat(body.location()).isEqualTo(ParameterLocation.BODY);
    assertThat(body.required()).isTrue();
    assertThat(body.schema().asMap())
        .containsEntry("type", "object")
        .extractingByKey("properties")
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsOnlyKeys("id", "name");
  }

  @Test
  void toToolCatalogOfAnOperationWithoutAnOperationId_skipsThatOperation() throws Exception {
    assertThat(toToolCatalog(MISSING_ID_SPEC).tools())
        .extracting(tool -> tool.name().value())
        .containsExactly("listUsers");
  }

  @Test
  void toToolCatalogOfAnOperationWithAnUnsupportedParameterLocation_skipsThatParameterOnly()
      throws Exception {
    Tool listUsers = toToolCatalog(COOKIE_SPEC).find(new ToolName("listUsers"));

    assertThat(listUsers.parameters()).extracting(ToolParameter::name).containsExactly("page");
  }

  @Test
  void toToolCatalogOfAnOperationWithReservedHeaderParameters_skipsThoseHeadersOnly()
      throws Exception {
    Tool listUsers = toToolCatalog(RESERVED_HEADER_SPEC).find(new ToolName("listUsers"));

    assertThat(listUsers.parameters())
        .extracting(ToolParameter::name)
        .containsExactly("X-Request-Id");
  }

  @Test
  void toToolCatalogOfAnOperationThatDeclaresOneNameTwice_keepsTheFirstDeclarationOnly() {
    Operation operation = new Operation().operationId("getUser");
    operation.addParametersItem(new PathParameter().name("id").schema(new StringSchema()));
    operation.addParametersItem(new QueryParameter().name("id").schema(new StringSchema()));

    Tool getUser =
        OpenApiToolCatalogMapper.toToolCatalog(specWith("/users/{id}", operation))
            .tools()
            .getFirst();

    assertThat(getUser.parameters())
        .extracting(ToolParameter::name, ToolParameter::location)
        .containsExactly(Tuple.tuple("id", ParameterLocation.PATH));
  }

  @Test
  void toToolCatalogOfARequestBodyWithoutJsonContent_leavesTheToolWithoutABody() throws Exception {
    assertThat(bodyOf(toToolCatalog(COOKIE_SPEC).find(new ToolName("createUser")))).isNull();
  }

  @Test
  void toToolCatalogOfASpecWithADuplicateOperationId_throwsNamingBothTargets() {
    assertThatThrownBy(() -> toToolCatalog(DUPLICATE_SPEC))
        .isInstanceOf(DuplicateToolNameException.class)
        .hasMessageContainingAll("listUsers", "/users", "/people");
  }

  @Test
  void toToolCatalogOfAParameterThatDeclaresTheBodyLocation_skipsThatParameter() {
    Operation operation = new Operation().operationId("createUser");
    operation.addParametersItem(
        new Parameter().name("payload").in("body").schema(new StringSchema()));

    Tool createUser =
        OpenApiToolCatalogMapper.toToolCatalog(specWith("/users", operation)).tools().getFirst();

    assertThat(createUser.parameters()).isEmpty();
  }

  @Test
  void toToolCatalogOfARequestBodyWhenAParameterAlreadyClaimsTheBodyName_keepsTheParameter() {
    Operation operation = new Operation().operationId("createUser");
    operation.addParametersItem(
        new QueryParameter().name(ToolParameter.BODY_NAME).schema(new StringSchema()));
    operation.setRequestBody(jsonRequestBody());

    Tool createUser =
        OpenApiToolCatalogMapper.toToolCatalog(specWith("/users", operation)).tools().getFirst();

    assertThat(createUser.parameters())
        .extracting(ToolParameter::name, ToolParameter::location)
        .containsExactly(Tuple.tuple(ToolParameter.BODY_NAME, ParameterLocation.QUERY));
  }

  @Test
  void toToolCatalogOfARequestBodyThatDeclaresNoRequiredFlag_leavesTheBodyArgumentOptional() {
    Operation operation = new Operation().operationId("createUser");
    operation.setRequestBody(jsonRequestBody());

    Tool createUser =
        OpenApiToolCatalogMapper.toToolCatalog(specWith("/users", operation)).tools().getFirst();

    ToolParameter body = bodyOf(createUser);
    assertThat(body).isNotNull();
    assertThat(body.required()).isFalse();
  }

  @Test
  void toToolCatalogOfAnEmptySpec_buildsAnEmptyCatalog() {
    assertThat(OpenApiToolCatalogMapper.toToolCatalog(new OpenAPI()).tools()).isEmpty();
  }

  private static RequestBody jsonRequestBody() {
    return new RequestBody()
        .content(
            new Content()
                .addMediaType("application/json", new MediaType().schema(new ObjectSchema())));
  }

  private static OpenAPI specWith(String path, Operation operation) {
    PathItem pathItem = new PathItem();
    pathItem.setGet(operation);
    Paths paths = new Paths();
    paths.addPathItem(path, pathItem);
    return new OpenAPI().paths(paths);
  }

  private static @Nullable ToolParameter bodyOf(Tool tool) {
    return tool.parameters().stream()
        .filter(parameter -> parameter.location() == ParameterLocation.BODY)
        .findFirst()
        .orElse(null);
  }

  private static ToolCatalog toToolCatalog(String specLocation) throws Exception {
    return new OpenApiToolCatalogSource(specLocation).catalog();
  }
}
