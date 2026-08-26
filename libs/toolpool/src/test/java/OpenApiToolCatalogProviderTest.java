import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.adapter.openapi.OpenApiToolCatalogProvider;
import io.github.flexksx.application.ToolCatalogUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class OpenApiToolCatalogProviderTest {

  private static final String MISSING_SPEC = "openapi-specs/no-such-spec.openapi.json";
  private static final String MALFORMED_SPEC = "openapi-specs/malformed.openapi.json";

  @ParameterizedTest
  @ValueSource(
      strings = {
        "openapi-specs/sample-rest-api-client.openapi.json",
        "openapi-specs/sample-rest-api-client.openapi.yaml"
      })
  void catalogOfAParsableSpec_holdsOneToolPerOperation(String specLocation) throws Exception {
    assertThat(new OpenApiToolCatalogProvider(specLocation).catalog().tools())
        .extracting(tool -> tool.name().value())
        .containsExactlyInAnyOrder("getUser", "createUser", "updateUser");
  }

  @Test
  void catalogTwice_readsTheSpecOnceAndServesOneCatalog() throws Exception {
    OpenApiToolCatalogProvider provider =
        new OpenApiToolCatalogProvider(ToolpoolFixtures.SAMPLE_SPEC);

    assertThat(provider.catalog()).isSameAs(provider.catalog());
  }

  @ParameterizedTest
  @ValueSource(strings = {MISSING_SPEC, MALFORMED_SPEC})
  void catalogOfAnUnreadableSpec_throwsNamingTheSpecLocation(String specLocation) {
    assertThatThrownBy(() -> new OpenApiToolCatalogProvider(specLocation).catalog())
        .isInstanceOf(ToolCatalogUnavailableException.class)
        .hasMessageContaining(specLocation);
  }
}
