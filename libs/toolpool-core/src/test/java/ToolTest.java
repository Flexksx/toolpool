import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.http.HttpMethod;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.MissingRequiredArgumentException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCall;
import io.github.flexksx.toolpool.domain.tool.ToolDocumentation;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ToolTest {

  private static final String KEYWORD_TYPE = "type";
  private static final String KEYWORD_PROPERTIES = "properties";
  private static final String KEYWORD_REQUIRED = "required";
  private static final String KEYWORD_DESCRIPTION = "description";
  private static final String TYPE_OBJECT = "object";
  private static final String TYPE_STRING = "string";
  private static final String TYPE_BOOLEAN = "boolean";

  private static final JsonSchema SCHEMA_STRING = new JsonSchema(Map.of(KEYWORD_TYPE, TYPE_STRING));
  private static final JsonSchema SCHEMA_BOOLEAN =
      new JsonSchema(Map.of(KEYWORD_TYPE, TYPE_BOOLEAN));
  private static final JsonSchema SCHEMA_OBJECT = new JsonSchema(Map.of(KEYWORD_TYPE, TYPE_OBJECT));

  private static final String ARGUMENT_IDENTIFIER = "id";
  private static final String ARGUMENT_VERBOSE = "verbose";
  private static final String ARGUMENT_REQUEST_ID = "X-Request-Id";
  private static final String DESCRIPTION_IDENTIFIER = "The identifier of the user";
  private static final String VALUE_IDENTIFIER = "u1";
  private static final String VALUE_REQUEST_ID = "r1";
  private static final Object VALUE_BODY = Map.of("name", "Ada");

  private static final String PATH_USER = "/users/{id}";
  private static final String DOCUMENTATION_SUMMARY = "Read one user";
  private static final String DOCUMENTATION_DESCRIPTION = "Answers the stored user";
  private static final String DOCUMENTATION_TAG = "People";

  private static final ToolName NAME_GET_USER = ToolName.of("getUser");
  private static final HttpTarget TARGET_GET_USER = new HttpTarget(HttpMethod.GET, PATH_USER);

  private static final ToolParameter PARAMETER_REQUIRED_PATH_IDENTIFIER =
      new ToolParameter(
          ARGUMENT_IDENTIFIER, ParameterLocation.PATH, true, SCHEMA_STRING, DESCRIPTION_IDENTIFIER);
  private static final ToolParameter PARAMETER_OPTIONAL_QUERY_VERBOSE =
      new ToolParameter(ARGUMENT_VERBOSE, ParameterLocation.QUERY, false, SCHEMA_BOOLEAN, null);
  private static final ToolParameter PARAMETER_OPTIONAL_HEADER_REQUEST_ID =
      new ToolParameter(ARGUMENT_REQUEST_ID, ParameterLocation.HEADER, false, SCHEMA_STRING, null);
  private static final ToolParameter PARAMETER_REQUIRED_BODY =
      ToolParameter.body(true, SCHEMA_OBJECT, null);
  private static final ToolParameter PARAMETER_OPTIONAL_BODY =
      ToolParameter.body(false, JsonSchema.empty(), null);

  private final Tool getUser =
      toolWith(
          List.of(
              PARAMETER_REQUIRED_PATH_IDENTIFIER,
              PARAMETER_OPTIONAL_QUERY_VERBOSE,
              PARAMETER_OPTIONAL_HEADER_REQUEST_ID));

  @Test
  void inputSchemaOfAToolWithParameters_describesEachOneAndRequiresOnlyTheRequiredOnes() {
    Map<String, Object> inputSchema = getUser.inputSchema().asMap();

    assertThat(inputSchema)
        .containsEntry(KEYWORD_TYPE, TYPE_OBJECT)
        .containsEntry(KEYWORD_REQUIRED, List.of(ARGUMENT_IDENTIFIER));
    assertThat(propertiesOf(inputSchema))
        .containsOnlyKeys(ARGUMENT_IDENTIFIER, ARGUMENT_VERBOSE, ARGUMENT_REQUEST_ID);
    assertThat(propertyOf(inputSchema, ARGUMENT_VERBOSE)).containsEntry(KEYWORD_TYPE, TYPE_BOOLEAN);
    assertThat(propertyOf(inputSchema, ARGUMENT_IDENTIFIER))
        .containsEntry(KEYWORD_DESCRIPTION, DESCRIPTION_IDENTIFIER);
  }

  @Test
  void inputSchemaOfAToolWithABody_nestsTheBodyUnderOneRequiredProperty() {
    Tool withRequiredBody =
        toolWith(List.of(PARAMETER_REQUIRED_PATH_IDENTIFIER, PARAMETER_REQUIRED_BODY));

    Map<String, Object> inputSchema = withRequiredBody.inputSchema().asMap();

    assertThat(propertiesOf(inputSchema))
        .containsOnlyKeys(ARGUMENT_IDENTIFIER, ToolParameter.BODY_NAME);
    assertThat(inputSchema)
        .containsEntry(KEYWORD_REQUIRED, List.of(ARGUMENT_IDENTIFIER, ToolParameter.BODY_NAME));
  }

  @Test
  void bindEveryArgument_sendsEachOneToItsOwnLocation() {
    ToolCall call =
        getUser.bind(
            Map.of(
                ARGUMENT_IDENTIFIER,
                VALUE_IDENTIFIER,
                ARGUMENT_VERBOSE,
                true,
                ARGUMENT_REQUEST_ID,
                VALUE_REQUEST_ID));

    assertThat(call.toolName()).isEqualTo(NAME_GET_USER);
    assertThat(call.target()).isEqualTo(TARGET_GET_USER);
    assertThat(call.pathVariables())
        .containsExactly(Map.entry(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER));
    assertThat(call.queryParameters())
        .containsExactly(Map.entry(ARGUMENT_VERBOSE, List.of("true")));
    assertThat(call.headers()).containsExactly(Map.entry(ARGUMENT_REQUEST_ID, VALUE_REQUEST_ID));
    assertThat(call.body()).isNull();
  }

  @Test
  void bindWithoutTheOptionalArguments_leavesThemOutOfTheCall() {
    ToolCall call = getUser.bind(Map.of(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER));

    assertThat(call.queryParameters()).isEmpty();
    assertThat(call.headers()).isEmpty();
  }

  @Test
  void bindAnArgumentThatNoParameterDeclares_leavesItOutOfTheCall() {
    ToolCall call = getUser.bind(Map.of(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER, "unknown", "value"));

    assertThat(call.queryParameters()).isEmpty();
    assertThat(call.headers()).isEmpty();
    assertThat(call.pathVariables()).containsOnlyKeys(ARGUMENT_IDENTIFIER);
  }

  @Test
  void bindWithoutARequiredParameter_throwsNamingTheLocationTheParameterAndTheTool() {
    assertThatThrownBy(() -> getUser.bind(Map.of(ARGUMENT_VERBOSE, true)))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContainingAll("path", ARGUMENT_IDENTIFIER, NAME_GET_USER.value());
  }

  @Test
  void bindNullArguments_reportsTheFirstMissingRequiredParameter() {
    assertThatThrownBy(() -> getUser.bind(null))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContaining(ARGUMENT_IDENTIFIER);
  }

  @Test
  void bindTheBodyArgument_sendsItToTheCallBody() {
    Tool withRequiredBody = toolWith(List.of(PARAMETER_REQUIRED_BODY));

    ToolCall call = withRequiredBody.bind(Map.of(ToolParameter.BODY_NAME, VALUE_BODY));

    assertThat(call.body()).isEqualTo(VALUE_BODY);
  }

  @Test
  void bindWithoutARequiredBody_throwsNamingTheBodyAndTheTool() {
    Tool withRequiredBody =
        toolWith(List.of(PARAMETER_REQUIRED_PATH_IDENTIFIER, PARAMETER_REQUIRED_BODY));

    assertThatThrownBy(() -> withRequiredBody.bind(Map.of(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER)))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContainingAll(ToolParameter.BODY_NAME, NAME_GET_USER.value());
  }

  @Test
  void bindWithoutAnOptionalBody_bindsTheCallWithoutABody() {
    Tool withOptionalBody =
        toolWith(List.of(PARAMETER_REQUIRED_PATH_IDENTIFIER, PARAMETER_OPTIONAL_BODY));

    assertThat(withOptionalBody.bind(Map.of(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER)).body())
        .isNull();
  }

  @ParameterizedTest
  @CsvSource({"getuser, true", "READ ONE, true", "stored, true", "people, true", "deleted, false"})
  void matchesAQuery_isTrueOnlyWhenTheNameSummaryDescriptionOrTagsContainIt(
      String query, boolean expected) {
    assertThat(documentedTool().matches(query)).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void matchesABlankQuery_isTrueForEveryTool(String query) {
    assertThat(getUser.matches(query)).isTrue();
  }

  @Test
  void descriptionOfADocumentedTool_joinsTheSummaryAndTheDescription() {
    assertThat(documentedTool().description()).isEqualTo("Read one user\nAnswers the stored user");
  }

  @Test
  void descriptionOfAnUndocumentedTool_fallsBackToTheMethodAndThePath() {
    assertThat(getUser.description()).isEqualTo("GET /users/{id}");
  }

  private static Tool documentedTool() {
    return new Tool(
        NAME_GET_USER,
        TARGET_GET_USER,
        new ToolDocumentation(
            DOCUMENTATION_SUMMARY, DOCUMENTATION_DESCRIPTION, List.of(DOCUMENTATION_TAG)),
        List.of());
  }

  private static Tool toolWith(List<ToolParameter> parameters) {
    return new Tool(
        NAME_GET_USER, TARGET_GET_USER, new ToolDocumentation(null, null, List.of()), parameters);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertiesOf(Map<String, Object> inputSchema) {
    return (Map<String, Object>) inputSchema.get(KEYWORD_PROPERTIES);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertyOf(Map<String, Object> inputSchema, String name) {
    return (Map<String, Object>) propertiesOf(inputSchema).get(name);
  }
}
