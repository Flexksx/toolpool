import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolDocumentation;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ToolSearchTest {

  private static final String DOCUMENTATION_SUMMARY = "Read one user";
  private static final String DOCUMENTATION_DESCRIPTION = "Answers the stored user";
  private static final String DOCUMENTATION_TAG = "People";

  private final Tool documentedTool =
      ToolExamples.toolDocumentedAs(
          new ToolDocumentation(
              DOCUMENTATION_SUMMARY, DOCUMENTATION_DESCRIPTION, List.of(DOCUMENTATION_TAG)));

  @ParameterizedTest
  @CsvSource({"getuser, true", "READ ONE, true", "stored, true", "people, true", "deleted, false"})
  void matchesAQuery_isTrueOnlyWhenTheNameSummaryDescriptionOrTagsContainIt(
      String query, boolean expected) {
    assertThat(documentedTool.matches(query)).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void matchesABlankQuery_isTrueForEveryTool(String query) {
    assertThat(ToolExamples.toolWith(List.of()).matches(query)).isTrue();
  }
}
