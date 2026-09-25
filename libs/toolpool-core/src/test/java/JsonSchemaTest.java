import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class JsonSchemaTest {

  private static final JsonSchema SCHEMA_STRING = new JsonSchema(Map.of("type", "string"));

  @Test
  void withADescriptionOnASchemaThatHasNone_addsIt() {
    assertThat(SCHEMA_STRING.withDescription("The identifier of the user").asMap())
        .containsEntry("description", "The identifier of the user");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void withABlankDescription_leavesTheSchemaUnchanged(String description) {
    assertThat(SCHEMA_STRING.withDescription(description)).isEqualTo(SCHEMA_STRING);
  }

  @Test
  void withADescriptionOnASchemaThatAlreadyHasOne_keepsTheOriginal() {
    JsonSchema described = new JsonSchema(Map.of("type", "string", "description", "From the spec"));

    assertThat(described.withDescription("From the parameter").asMap())
        .containsEntry("description", "From the spec");
  }

  @Test
  void asMapOfASchema_rejectsMutation() {
    Map<String, Object> asMap = SCHEMA_STRING.asMap();

    assertThatThrownBy(() -> asMap.put("type", "array"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void constructFromAMutableMap_ignoresLaterChangesToThatMap() {
    Map<String, Object> source = new LinkedHashMap<>(Map.of("type", "string"));
    JsonSchema schema = new JsonSchema(source);

    source.put("type", "integer");

    assertThat(schema.asMap()).containsEntry("type", "string");
  }
}
