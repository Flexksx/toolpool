import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.MissingRequiredArgumentException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ToolRequestTest {

  private static final String ARGUMENT_IDENTIFIER = "id";
  private static final String ARGUMENT_VERBOSE = "verbose";
  private static final String ARGUMENT_REQUEST_ID = "X-Request-Id";
  private static final String ARGUMENT_UNDECLARED = "unknown";
  private static final String VALUE_IDENTIFIER = "u1";
  private static final String VALUE_REQUEST_ID = "r1";
  private static final String VALUE_UNDECLARED = "value";
  private static final String LOCATION_PATH = "path";
  private static final Object VALUE_BODY = Map.of("name", "Ada");

  private static final ToolParameter PARAMETER_REQUIRED_PATH_IDENTIFIER =
      new ToolParameter(
          ARGUMENT_IDENTIFIER, ParameterLocation.PATH, true, JsonSchema.stringType(), null);
  private static final ToolParameter PARAMETER_OPTIONAL_QUERY_VERBOSE =
      new ToolParameter(ARGUMENT_VERBOSE, ParameterLocation.QUERY, false, JsonSchema.empty(), null);
  private static final ToolParameter PARAMETER_OPTIONAL_HEADER_REQUEST_ID =
      new ToolParameter(
          ARGUMENT_REQUEST_ID, ParameterLocation.HEADER, false, JsonSchema.stringType(), null);
  private static final ToolParameter PARAMETER_REQUIRED_BODY =
      ToolParameter.body(true, JsonSchema.objectType(), null);
  private static final ToolParameter PARAMETER_OPTIONAL_BODY =
      ToolParameter.body(false, JsonSchema.empty(), null);

  private final Tool getUser =
      ToolExamples.toolWith(
          List.of(
              PARAMETER_REQUIRED_PATH_IDENTIFIER,
              PARAMETER_OPTIONAL_QUERY_VERBOSE,
              PARAMETER_OPTIONAL_HEADER_REQUEST_ID));

  @Test
  void requestForEveryArgument_sendsEachOneToItsOwnLocation() {
    ToolCallRequest call =
        getUser.requestFor(
            Map.of(
                ARGUMENT_IDENTIFIER,
                VALUE_IDENTIFIER,
                ARGUMENT_VERBOSE,
                true,
                ARGUMENT_REQUEST_ID,
                VALUE_REQUEST_ID));

    assertThat(call.toolName()).isEqualTo(ToolExamples.NAME_GET_USER);
    assertThat(call.target()).isEqualTo(ToolExamples.TARGET_GET_USER);
    assertThat(call.pathVariables())
        .containsExactly(Map.entry(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER));
    assertThat(call.queryParameters())
        .containsExactly(Map.entry(ARGUMENT_VERBOSE, List.of(String.valueOf(true))));
    assertThat(call.headers()).containsExactly(Map.entry(ARGUMENT_REQUEST_ID, VALUE_REQUEST_ID));
    assertThat(call.body()).isNull();
  }

  @Test
  void requestForNoOptionalArguments_leavesThemOut() {
    ToolCallRequest call = getUser.requestFor(Map.of(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER));

    assertThat(call.queryParameters()).isEmpty();
    assertThat(call.headers()).isEmpty();
  }

  @Test
  void requestForAnArgumentThatNoParameterDeclares_leavesItOut() {
    ToolCallRequest call =
        getUser.requestFor(
            Map.of(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER, ARGUMENT_UNDECLARED, VALUE_UNDECLARED));

    assertThat(call.queryParameters()).isEmpty();
    assertThat(call.headers()).isEmpty();
    assertThat(call.pathVariables()).containsOnlyKeys(ARGUMENT_IDENTIFIER);
  }

  @Test
  void requestForMissingRequiredParameter_throwsNamingTheLocationTheParameterAndTheTool() {
    assertThatThrownBy(() -> getUser.requestFor(Map.of(ARGUMENT_VERBOSE, true)))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContainingAll(
            LOCATION_PATH, ARGUMENT_IDENTIFIER, ToolExamples.NAME_GET_USER.value());
  }

  @Test
  void requestForNullArguments_reportsTheFirstMissingRequiredParameter() {
    assertThatThrownBy(() -> getUser.requestFor(null))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContaining(ARGUMENT_IDENTIFIER);
  }

  @Test
  void requestForTheBodyArgument_sendsItToTheRequestBody() {
    Tool withRequiredBody = ToolExamples.toolWith(List.of(PARAMETER_REQUIRED_BODY));

    ToolCallRequest call = withRequiredBody.requestFor(Map.of(ToolParameter.BODY_NAME, VALUE_BODY));

    assertThat(call.body()).isEqualTo(VALUE_BODY);
  }

  @Test
  void requestForMissingRequiredBody_throwsNamingTheBodyAndTheTool() {
    Tool withRequiredBody =
        ToolExamples.toolWith(List.of(PARAMETER_REQUIRED_PATH_IDENTIFIER, PARAMETER_REQUIRED_BODY));

    assertThatThrownBy(
            () -> withRequiredBody.requestFor(Map.of(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER)))
        .isInstanceOf(MissingRequiredArgumentException.class)
        .hasMessageContainingAll(ToolParameter.BODY_NAME, ToolExamples.NAME_GET_USER.value());
  }

  @Test
  void requestForMissingOptionalBody_carriesNoBody() {
    Tool withOptionalBody =
        ToolExamples.toolWith(List.of(PARAMETER_REQUIRED_PATH_IDENTIFIER, PARAMETER_OPTIONAL_BODY));

    assertThat(withOptionalBody.requestFor(Map.of(ARGUMENT_IDENTIFIER, VALUE_IDENTIFIER)).body())
        .isNull();
  }
}
