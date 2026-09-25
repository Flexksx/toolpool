import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ToolParameterTest {

  private static final String NAME_IDENTIFIER = "id";
  private static final String NAME_PAYLOAD = "payload";
  private static final String DESCRIPTION_IDENTIFIER = "The identifier of the user";
  private static final String DESCRIPTION_PREVIOUS = "A description that gets replaced";
  private static final JsonSchema SCHEMA_STRING = JsonSchema.stringType();
  private static final JsonSchema SCHEMA_OBJECT = JsonSchema.objectType();

  private static final ToolParameter PARAMETER_PATH_IDENTIFIER =
      ToolParameter.inPath(NAME_IDENTIFIER, SCHEMA_STRING);

  @ParameterizedTest
  @MethodSource("factories")
  void factory_buildsAnOptionalUndescribedParameterAtItsLocation(
      Supplier<ToolParameter> factory, ToolParameter expected) {
    assertThat(factory.get()).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void withRequired_setsTheFlagAndKeepsEveryOtherValue(boolean required) {
    ToolParameter original =
        new ToolParameter(
            NAME_IDENTIFIER,
            ParameterLocation.PATH,
            !required,
            SCHEMA_STRING,
            DESCRIPTION_IDENTIFIER);

    assertThat(original.withRequired(required))
        .isEqualTo(
            new ToolParameter(
                NAME_IDENTIFIER,
                ParameterLocation.PATH,
                required,
                SCHEMA_STRING,
                DESCRIPTION_IDENTIFIER));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = DESCRIPTION_IDENTIFIER)
  void withDescription_replacesTheDescriptionAndKeepsEveryOtherValue(String description) {
    ToolParameter original =
        new ToolParameter(
            NAME_IDENTIFIER, ParameterLocation.PATH, true, SCHEMA_STRING, DESCRIPTION_PREVIOUS);

    assertThat(original.withDescription(description))
        .isEqualTo(
            new ToolParameter(
                NAME_IDENTIFIER, ParameterLocation.PATH, true, SCHEMA_STRING, description));
  }

  @Test
  void withRequired_leavesTheOriginalUnchanged() {
    PARAMETER_PATH_IDENTIFIER.withRequired(true);

    assertThat(PARAMETER_PATH_IDENTIFIER.required()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void constructABlankName_throwsNamingTheRule(String name) {
    assertThatThrownBy(() -> ToolParameter.inQuery(name, SCHEMA_STRING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be blank");
  }

  @Test
  void constructABodyWithAnotherName_throwsNamingTheExpectedName() {
    assertThatThrownBy(
            () ->
                new ToolParameter(NAME_PAYLOAD, ParameterLocation.BODY, false, SCHEMA_OBJECT, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be named " + ToolParameter.BODY_NAME);
  }

  private static Stream<Arguments> factories() {
    return Stream.of(
        factoryCase(
            "inPath",
            () -> ToolParameter.inPath(NAME_IDENTIFIER, SCHEMA_STRING),
            NAME_IDENTIFIER,
            ParameterLocation.PATH),
        factoryCase(
            "inQuery",
            () -> ToolParameter.inQuery(NAME_IDENTIFIER, SCHEMA_STRING),
            NAME_IDENTIFIER,
            ParameterLocation.QUERY),
        factoryCase(
            "inHeader",
            () -> ToolParameter.inHeader(NAME_IDENTIFIER, SCHEMA_STRING),
            NAME_IDENTIFIER,
            ParameterLocation.HEADER),
        factoryCase(
            "inBody",
            () -> ToolParameter.inBody(SCHEMA_STRING),
            ToolParameter.BODY_NAME,
            ParameterLocation.BODY));
  }

  private static Arguments factoryCase(
      String factoryName,
      Supplier<ToolParameter> factory,
      String expectedName,
      ParameterLocation expectedLocation) {
    return Arguments.of(
        Named.of(factoryName, factory),
        new ToolParameter(expectedName, expectedLocation, false, SCHEMA_STRING, null));
  }
}
