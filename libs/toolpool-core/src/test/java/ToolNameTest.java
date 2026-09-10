import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.tool.InvalidToolNameException;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ToolNameTest {

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void constructABlankName_throwsInvalidToolName(String value) {
    assertThatThrownBy(() -> new ToolName(value))
        .isInstanceOf(InvalidToolNameException.class)
        .hasMessageContaining("cannot be blank");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "getUser",
        "get /users/{id}",
        "café",
        "list.users",
        "v1/getUsers",
        "keep-underscores_and-dashes"
      })
  void constructAName_keepsEveryCharacterBecauseTheProtocolOwnsTheNameRules(String value) {
    assertThat(new ToolName(value).value()).isEqualTo(value);
  }

  @Test
  void constructAVeryLongName_isValidBecauseTheDomainSetsNoLimit() {
    String value = "o".repeat(200);

    assertThat(new ToolName(value).value()).isEqualTo(value);
  }
}
