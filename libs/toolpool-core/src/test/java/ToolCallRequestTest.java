import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ToolCallRequestTest {

  @Test
  void constructWithoutATarget_throwsNamingTheTarget() {
    assertThatThrownBy(
            () ->
                new ToolCallRequest(
                    new ToolName("getUser"), null, Map.of(), Map.of(), Map.of(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("needs a target");
  }
}
