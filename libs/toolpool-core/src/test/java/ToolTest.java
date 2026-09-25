import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.ToolDocumentation;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ToolTest {

  private static final String ARGUMENT_IDENTIFIER = "id";
  private static final String DOCUMENTATION_SUMMARY = "Read one user";
  private static final String DOCUMENTATION_DESCRIPTION = "Answers the stored user";

  private static final ToolParameter PARAMETER_PATH_IDENTIFIER =
      new ToolParameter(
          ARGUMENT_IDENTIFIER, ParameterLocation.PATH, true, JsonSchema.stringType(), null);
  private static final ToolParameter PARAMETER_QUERY_IDENTIFIER =
      new ToolParameter(
          ARGUMENT_IDENTIFIER, ParameterLocation.QUERY, false, JsonSchema.stringType(), null);
  private static final ToolParameter PARAMETER_REQUIRED_BODY =
      ToolParameter.body(true, JsonSchema.objectType(), null);
  private static final ToolParameter PARAMETER_OPTIONAL_BODY =
      ToolParameter.body(false, JsonSchema.empty(), null);

  @Test
  void constructWithTwoParametersOfTheSameName_throwsNamingTheParameter() {
    assertThatThrownBy(
            () ->
                ToolExamples.toolWith(
                    List.of(PARAMETER_PATH_IDENTIFIER, PARAMETER_QUERY_IDENTIFIER)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("declares the parameter " + ARGUMENT_IDENTIFIER + " twice");
  }

  @Test
  void constructWithTwoBodies_throwsNamingTheBodyTwice() {
    assertThatThrownBy(
            () -> ToolExamples.toolWith(List.of(PARAMETER_REQUIRED_BODY, PARAMETER_OPTIONAL_BODY)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("declares the parameter " + ToolParameter.BODY_NAME + " twice");
  }

  @Test
  void descriptionOfADocumentedTool_joinsTheSummaryAndTheDescription() {
    ToolDocumentation documentation =
        new ToolDocumentation(DOCUMENTATION_SUMMARY, DOCUMENTATION_DESCRIPTION, List.of());

    assertThat(ToolExamples.toolDocumentedAs(documentation).description())
        .isEqualTo(DOCUMENTATION_SUMMARY + "\n" + DOCUMENTATION_DESCRIPTION);
  }

  @Test
  void descriptionOfAnUndocumentedTool_fallsBackToTheMethodAndThePath() {
    assertThat(ToolExamples.toolWith(List.of()).description())
        .isEqualTo(ToolExamples.TARGET_GET_USER.describe());
  }
}
