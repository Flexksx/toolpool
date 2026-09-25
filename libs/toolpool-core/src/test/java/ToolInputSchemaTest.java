import static io.github.flexksx.toolpool.domain.schema.JsonSchema.BOOLEAN_TYPE;
import static io.github.flexksx.toolpool.domain.schema.JsonSchema.DESCRIPTION_KEYWORD;
import static io.github.flexksx.toolpool.domain.schema.JsonSchema.OBJECT_TYPE;
import static io.github.flexksx.toolpool.domain.schema.JsonSchema.PROPERTIES_KEYWORD;
import static io.github.flexksx.toolpool.domain.schema.JsonSchema.REQUIRED_KEYWORD;
import static io.github.flexksx.toolpool.domain.schema.JsonSchema.TYPE_KEYWORD;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ToolInputSchemaTest {

  private static final String ARGUMENT_IDENTIFIER = "id";
  private static final String ARGUMENT_VERBOSE = "verbose";
  private static final String ARGUMENT_REQUEST_ID = "X-Request-Id";
  private static final String DESCRIPTION_IDENTIFIER = "The identifier of the user";

  private static final ToolParameter PARAMETER_REQUIRED_PATH_IDENTIFIER =
      ToolParameter.inPath(ARGUMENT_IDENTIFIER, JsonSchema.stringType())
          .withRequired(true)
          .withDescription(DESCRIPTION_IDENTIFIER);
  private static final ToolParameter PARAMETER_OPTIONAL_QUERY_VERBOSE =
      ToolParameter.inQuery(ARGUMENT_VERBOSE, new JsonSchema(Map.of(TYPE_KEYWORD, BOOLEAN_TYPE)));
  private static final ToolParameter PARAMETER_OPTIONAL_HEADER_REQUEST_ID =
      ToolParameter.inHeader(ARGUMENT_REQUEST_ID, JsonSchema.stringType());
  private static final ToolParameter PARAMETER_REQUIRED_BODY =
      ToolParameter.inBody(JsonSchema.objectType()).withRequired(true);

  @Test
  void inputSchemaOfAToolWithParameters_describesEachOneAndRequiresOnlyTheRequiredOnes() {
    Tool getUser =
        ToolExamples.toolWith(
            List.of(
                PARAMETER_REQUIRED_PATH_IDENTIFIER,
                PARAMETER_OPTIONAL_QUERY_VERBOSE,
                PARAMETER_OPTIONAL_HEADER_REQUEST_ID));

    Map<String, Object> inputSchema = getUser.inputSchema().asMap();

    assertThat(inputSchema)
        .containsEntry(TYPE_KEYWORD, OBJECT_TYPE)
        .containsEntry(REQUIRED_KEYWORD, List.of(ARGUMENT_IDENTIFIER));
    assertThat(propertiesOf(inputSchema))
        .containsOnlyKeys(ARGUMENT_IDENTIFIER, ARGUMENT_VERBOSE, ARGUMENT_REQUEST_ID);
    assertThat(propertyOf(inputSchema, ARGUMENT_VERBOSE)).containsEntry(TYPE_KEYWORD, BOOLEAN_TYPE);
    assertThat(propertyOf(inputSchema, ARGUMENT_IDENTIFIER))
        .containsEntry(DESCRIPTION_KEYWORD, DESCRIPTION_IDENTIFIER);
  }

  @Test
  void inputSchemaOfAToolWithABody_nestsTheBodyUnderOneRequiredProperty() {
    Tool withRequiredBody =
        ToolExamples.toolWith(List.of(PARAMETER_REQUIRED_PATH_IDENTIFIER, PARAMETER_REQUIRED_BODY));

    Map<String, Object> inputSchema = withRequiredBody.inputSchema().asMap();

    assertThat(propertiesOf(inputSchema))
        .containsOnlyKeys(ARGUMENT_IDENTIFIER, ToolParameter.BODY_NAME);
    assertThat(inputSchema)
        .containsEntry(REQUIRED_KEYWORD, List.of(ARGUMENT_IDENTIFIER, ToolParameter.BODY_NAME));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertiesOf(Map<String, Object> inputSchema) {
    return (Map<String, Object>) inputSchema.get(PROPERTIES_KEYWORD);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertyOf(Map<String, Object> inputSchema, String name) {
    return (Map<String, Object>) propertiesOf(inputSchema).get(name);
  }
}
