import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import org.junit.jupiter.api.Test;

public class ToolParameterTest {

  @Test
  void constructABodyWithAnotherName_throwsNamingTheExpectedName() {
    assertThatThrownBy(
            () ->
                new ToolParameter(
                    "payload", ParameterLocation.BODY, false, JsonSchema.objectType(), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be named body");
  }

  @Test
  void body_namesTheParameterBody() {
    assertThat(ToolParameter.body(true, JsonSchema.objectType(), null).name())
        .isEqualTo(ToolParameter.BODY_NAME);
  }
}
