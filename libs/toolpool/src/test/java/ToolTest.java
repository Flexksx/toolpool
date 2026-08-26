import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.domain.http.HttpMethod;
import io.github.flexksx.domain.http.HttpTarget;
import io.github.flexksx.domain.http.ParameterLocation;
import io.github.flexksx.domain.schema.JsonSchema;
import io.github.flexksx.domain.tool.MissingRequiredArgumentException;
import io.github.flexksx.domain.tool.Tool;
import io.github.flexksx.domain.tool.ToolBody;
import io.github.flexksx.domain.tool.ToolCall;
import io.github.flexksx.domain.tool.ToolDocumentation;
import io.github.flexksx.domain.tool.ToolName;
import io.github.flexksx.domain.tool.ToolParameter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ToolTest {

  private static final ToolName GET_USER = ToolName.of("getUser");
  private static final HttpTarget TARGET = new HttpTarget(HttpMethod.GET, "/users/{id}");
  private static final ToolParameter REQUIRED_PATH_ID =
      new ToolParameter(
          "id", ParameterLocation.PATH, true, stringSchema(), "The identifier of the user");
  private static final ToolParameter OPTIONAL_QUERY_VERBOSE =
      new ToolParameter("verbose", ParameterLocation.QUERY, false, booleanSchema(), null);
  private static final ToolParameter OPTIONAL_HEADER_REQUEST_ID =
      new ToolParameter("X-Request-Id", ParameterLocation.HEADER, false, stringSchema(), null);

  private final Tool getUser =
      toolWith(List.of(REQUIRED_PATH_ID, OPTIONAL_QUERY_VERBOSE, OPTIONAL_HEADER_REQUEST_ID), null);

  @Test
  void inputSchemaOfAToolWithParameters_describesEachOneAndRequiresOnlyTheRequiredOnes() {
    Map<String, Object> inputSchema = getUser.inputSchema().asMap();

    assertThat(inputSchema)
        .containsEntry("type", "object")
        .containsEntry("required", List.of("id"));
    assertThat(propertiesOf(inputSchema)).containsOnlyKeys("id", "verbose", "X-Request-Id");
    assertThat(propertyOf(inputSchema, "verbose")).containsEntry("type", "boolean");
    assertThat(propertyOf(inputSchema, "id"))
        .containsEntry("description", "The identifier of the user");
  }

  @Test
  void inputSchemaOfAToolWithABody_nestsTheBodyUnderOneRequiredProperty() {
    Tool updateUser =
        toolWith(
            List.of(REQUIRED_PATH_ID),
            new ToolBody(true, new JsonSchema(Map.of("type", "object")), null));

    Map<String, Object> inputSchema = updateUser.inputSchema().asMap();

    assertThat(propertiesOf(inputSchema)).containsOnlyKeys("id", Tool.BODY_ARGUMENT);
    assertThat(inputSchema).containsEntry("required", List.of("id", Tool.BODY_ARGUMENT));
  }

  @Test
  void inputSchemaMap_rejectsMutation() {
    Map<String, Object> inputSchema = getUser.inputSchema().asMap();

    assertThatThrownBy(() -> inputSchema.put("type", "array"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void bindEveryArgument_sendsEachOneToItsOwnLocation() {
    ToolCall call = getUser.bind(Map.of("id", "u1", "verbose", true, "X-Request-Id", "r1"));

    assertThat(call.toolName()).isEqualTo(GET_USER);
    assertThat(call.target()).isEqualTo(TARGET);
    assertThat(call.pathVariables()).containsExactly(Map.entry("id", "u1"));
    assertThat(call.queryParameters()).containsExactly(Map.entry("verbose", List.of("true")));
    assertThat(call.headers()).containsExactly(Map.entry("X-Request-Id", "r1"));
    assertThat(call.body()).isNull();
  }

  @Test
  void bindWithoutTheOptionalArguments_leavesThemOutOfTheCall() {
    ToolCall call = getUser.bind(Map.of("id", "u1"));

    assertThat(call.queryParameters()).isEmpty();
    assertThat(call.headers()).isEmpty();
  }

  @Test
  void bindWithoutARequiredParameter_throwsNamingTheLocationTheParameterAndTheTool() {
    assertThatThrownBy(() -> getUser.bind(Map.of("verbose", true)))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContainingAll("path", "id", "getUser");
  }

  @Test
  void bindWithoutARequiredBody_throwsNamingTheBodyAndTheTool() {
    Tool updateUser =
        toolWith(List.of(REQUIRED_PATH_ID), new ToolBody(true, JsonSchema.empty(), null));

    assertThatThrownBy(() -> updateUser.bind(Map.of("id", "u1")))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContainingAll(Tool.BODY_ARGUMENT, "getUser");
  }

  @Test
  void bindWithoutAnOptionalBody_bindsTheCallWithoutABody() {
    Tool updateUser =
        toolWith(List.of(REQUIRED_PATH_ID), new ToolBody(false, JsonSchema.empty(), null));

    assertThat(updateUser.bind(Map.of("id", "u1")).body()).isNull();
  }

  @Test
  void bindNullArguments_reportsTheFirstMissingRequiredParameter() {
    assertThatThrownBy(() -> getUser.bind(null))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContaining("id");
  }

  @Test
  void matchesAQueryThatAppearsInTheNameSummaryDescriptionOrTags_isTrue() {
    Tool tool = documentedTool(List.of("People"));

    assertThat(tool.matches("getuser")).isTrue();
    assertThat(tool.matches("READ ONE")).isTrue();
    assertThat(tool.matches("stored")).isTrue();
    assertThat(tool.matches("people")).isTrue();
    assertThat(tool.matches("deleted")).isFalse();
  }

  @Test
  void matchesABlankQuery_isTrueForEveryTool() {
    assertThat(getUser.matches("")).isTrue();
    assertThat(getUser.matches(null)).isTrue();
  }

  @Test
  void descriptionOfADocumentedTool_joinsTheSummaryAndTheDescription() {
    assertThat(documentedTool(List.of()).description())
        .isEqualTo("Read one user\nAnswers the stored user");
  }

  @Test
  void descriptionOfAnUndocumentedTool_fallsBackToTheMethodAndThePath() {
    assertThat(getUser.description()).isEqualTo("GET /users/{id}");
  }

  private static Tool documentedTool(List<String> tags) {
    return new Tool(
        GET_USER,
        TARGET,
        new ToolDocumentation("Read one user", "Answers the stored user", tags),
        List.of(),
        null);
  }

  private static Tool toolWith(List<ToolParameter> parameters, ToolBody body) {
    return new Tool(
        GET_USER, TARGET, new ToolDocumentation(null, null, List.of()), parameters, body);
  }

  private static JsonSchema stringSchema() {
    return new JsonSchema(Map.of("type", "string"));
  }

  private static JsonSchema booleanSchema() {
    return new JsonSchema(Map.of("type", "boolean"));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertiesOf(Map<String, Object> inputSchema) {
    return (Map<String, Object>) inputSchema.get("properties");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertyOf(Map<String, Object> inputSchema, String name) {
    return (Map<String, Object>) propertiesOf(inputSchema).get(name);
  }
}
