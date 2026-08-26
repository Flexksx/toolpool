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
    "'get /users/{id}', get__users__id_",
    "list.users, list_users",
    "keep-underscores_and-dashes, keep-underscores_and-dashes"
  })
  void ofARawName_replacesEveryCharacterThatMcpToolNamesDisallow(
      String rawName, String expectedValue) {
    assertThat(ToolName.of(rawName).value()).isEqualTo(expectedValue);
  }

  @Test
  void ofARawNameLongerThanTheLimit_truncatesItToTheLimit() {
    String rawName = "o".repeat(ToolName.MAX_LENGTH + 10);

    assertThat(ToolName.of(rawName).value()).isEqualTo("o".repeat(ToolName.MAX_LENGTH));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "has spaces", "has.dots"})
  void constructAnUnusableName_throwsInvalidToolName(String value) {
    assertThatThrownBy(() -> new ToolName(value)).isInstanceOf(InvalidToolNameException.class);
  }

  @Test
  void constructANameLongerThanTheLimit_throwsInvalidToolName() {
    String value = "o".repeat(ToolName.MAX_LENGTH + 1);

    assertThatThrownBy(() -> new ToolName(value))
        .isInstanceOf(InvalidToolNameException.class)
        .hasMessageContaining(String.valueOf(ToolName.MAX_LENGTH));
  }
}
