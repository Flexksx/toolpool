import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.openapi.OpenApiSpecReadException;
import io.github.flexksx.openapi.SwaggerOpenApiSpecReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class SwaggerOpenApiSpecReaderTest {

  private static final String MISSING_SPEC = "openapi-specs/no-such-spec.openapi.json";
  private static final String MALFORMED_SPEC = "openapi-specs/malformed.openapi.json";

  private final SwaggerOpenApiSpecReader reader = new SwaggerOpenApiSpecReader();

  @ParameterizedTest
  @CsvSource({
    "openapi-specs/sample-rest-api-client.openapi.json, JSON",
    "openapi-specs/sample-rest-api-client.openapi.yaml, YAML"
  })
  void readAParsableSpec_returnsTheParsedSpec(String specLocation, String expectedTitle)
      throws Exception {
    assertThat(reader.read(specLocation).getInfo().getTitle()).isEqualTo(expectedTitle);
  }

  @ParameterizedTest
  @ValueSource(strings = {MISSING_SPEC, MALFORMED_SPEC})
  void readAnUnparsableSpec_throwsNamingTheSpecLocation(String specLocation) {
    assertThatThrownBy(() -> reader.read(specLocation))
        .isInstanceOf(OpenApiSpecReadException.class)
        .hasMessageContaining(specLocation);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void readABlankSpecLocation_throwsIllegalArgument(String specLocation) {
    assertThatThrownBy(() -> reader.read(specLocation))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void readANullSpecLocation_throwsIllegalArgument() {
    assertThatThrownBy(() -> reader.read(null)).isInstanceOf(IllegalArgumentException.class);
  }
}
