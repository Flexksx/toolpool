import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.adapter.mcp.McpToolNames;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class McpToolNamesTest {

  @ParameterizedTest
  @CsvSource({
    "getUser, getUser",
    "'get /users/{id}', 'get_/users/_id_'",
    "list.users, list.users",
    "v1/getUsers, v1/getUsers",
    "keep-underscores_and-dashes, keep-underscores_and-dashes",
    "café, caf_"
  })
  void publishedNameOf_replacesEveryCharacterThatMcpToolNamesDisallow(
      String rawName, String expectedName) {
    assertThat(McpToolNames.publishedNameOf(new ToolName(rawName))).isEqualTo(expectedName);
  }

  @Test
  void publishedNameOfANameLongerThanTheLimit_truncatesItToTheLimit() {
    ToolName name = new ToolName("o".repeat(McpToolNames.MAX_CHARACTERS_LENGTH + 10));

    assertThat(McpToolNames.publishedNameOf(name))
        .isEqualTo("o".repeat(McpToolNames.MAX_CHARACTERS_LENGTH));
  }

  @Test
  void publishedNameOfANameOfTheMaximumLength_keepsEveryCharacter() {
    String value = "a1.b/c-d".repeat(8);

    assertThat(McpToolNames.publishedNameOf(new ToolName(value))).isEqualTo(value);
  }

  @Test
  void publishedNameOfAMixedCaseName_keepsTheOriginalCase() {
    assertThat(McpToolNames.publishedNameOf(new ToolName("GeT_UsEr-1.2/3")))
        .isEqualTo("GeT_UsEr-1.2/3");
  }
}
