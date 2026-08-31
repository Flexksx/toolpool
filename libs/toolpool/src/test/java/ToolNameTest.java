import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.domain.tool.InvalidToolNameException;
import io.github.flexksx.domain.tool.ToolName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void constructABlankName_throwsInvalidToolName(String value) {
    assertThatThrownBy(() -> new ToolName(value))
        .isInstanceOf(InvalidToolNameException.class)
        .hasMessageContaining("cannot be blank");
  }

  @ParameterizedTest
  @ValueSource(strings = {"has spaces", "has{braces}", "café"})
  void constructANameWithDisallowedCharacters_throwsInvalidToolName(String value) {
    assertThatThrownBy(() -> new ToolName(value))
        .isInstanceOf(InvalidToolNameException.class)
        .hasMessageContaining("other than a-z A-Z 0-9 _ . / -");
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
}
