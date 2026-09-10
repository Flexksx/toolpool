import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.tool.InvalidToolNameException;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class ToolNameTest {

  @ParameterizedTest
  @CsvSource({
    "getUser, getUser",
    "'get /users/{id}', 'get_/users/_id_'",
    "list.users, list.users",
    "v1/getUsers, v1/getUsers",
    "keep-underscores_and-dashes, keep-underscores_and-dashes"
  })
  void ofARawName_replacesEveryCharacterThatMcpToolNamesDisallow(
      String rawName, String expectedValue) {
    assertThat(ToolName.of(rawName).value()).isEqualTo(expectedValue);
  }

  @Test
  void ofARawNameLongerThanTheLimit_truncatesItToTheLimit() {
    String rawName = "o".repeat(ToolName.MCP_TOOL_MAX_CHARACTERS_LENGTH + 10);

    assertThat(ToolName.of(rawName).value())
        .isEqualTo("o".repeat(ToolName.MCP_TOOL_MAX_CHARACTERS_LENGTH));
  }

  @Test
  void ofARawNameWithMixedCase_keepsTheOriginalCase() {
    assertThat(ToolName.of("GeT_UsEr-1.2/3").value()).isEqualTo("GeT_UsEr-1.2/3");
  }

  private static Stream<InvalidNameCase> invalidNames() {
    return Stream.of(
        new InvalidNameCase("", InvalidToolNameException.class, "cannot be blank"),
        new InvalidNameCase("   ", InvalidToolNameException.class, "cannot be blank"),
        new InvalidNameCase(
            "has spaces", InvalidToolNameException.class, "other than a-z A-Z 0-9 _ . / -"),
        new InvalidNameCase(
            "has{braces}", InvalidToolNameException.class, "other than a-z A-Z 0-9 _ . / -"),
        new InvalidNameCase(
            "café", InvalidToolNameException.class, "other than a-z A-Z 0-9 _ . / -"));
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  void constructAnInvalidName_throwsWithExpectedMessage(InvalidNameCase testCase) {
    assertThatThrownBy(() -> new ToolName(testCase.rawName()))
        .isInstanceOf(testCase.expectedException())
        .hasMessageContaining(testCase.expectedMessage());
  }

  @Test
  void constructANameLongerThanTheLimit_throwsInvalidToolName() {
    String value = "o".repeat(ToolName.MCP_TOOL_MAX_CHARACTERS_LENGTH + 1);

    assertThatThrownBy(() -> new ToolName(value))
        .isInstanceOf(InvalidToolNameException.class)
        .hasMessageContaining(String.valueOf(ToolName.MCP_TOOL_MAX_CHARACTERS_LENGTH));
  }

  @Test
  void constructANameOfTheMaximumLength_isValid() {
    String value = "a1.b/c-d".repeat(8);

    assertThat(new ToolName(value).value()).isEqualTo(value);
  }

  @Test
  void constructANameOfOneCharacter_isValid() {
    assertThat(new ToolName("_").value()).isEqualTo("_");
  }

  private record InvalidNameCase(
      String rawName, Class<? extends Throwable> expectedException, String expectedMessage) {}
}
