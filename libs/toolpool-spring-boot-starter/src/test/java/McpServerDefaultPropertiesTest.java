import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.spring.McpServerDefaultProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

public class McpServerDefaultPropertiesTest {

  private static final String PROTOCOL_PROPERTY = "spring.ai.mcp.server.protocol";

  private final McpServerDefaultProperties defaultProperties = new McpServerDefaultProperties();

  @Test
  void postProcessAnEnvironmentWithoutAProtocol_defaultsToStreamableHttp() {
    StandardEnvironment environment = new StandardEnvironment();

    defaultProperties.postProcessEnvironment(environment, null);

    assertThat(environment.getProperty(PROTOCOL_PROPERTY)).isEqualTo("STREAMABLE");
  }

  @Test
  void postProcessAnEnvironmentThatAlreadySetsAProtocol_keepsTheConfiguredValue() {
    StandardEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(new MapPropertySource("test", Map.of(PROTOCOL_PROPERTY, "STATELESS")));

    defaultProperties.postProcessEnvironment(environment, null);

    assertThat(environment.getProperty(PROTOCOL_PROPERTY)).isEqualTo("STATELESS");
  }
}
