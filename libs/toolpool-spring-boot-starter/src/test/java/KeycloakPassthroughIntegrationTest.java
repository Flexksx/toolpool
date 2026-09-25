import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.testing.ToolpoolFixtures;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = AuthTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
      "server.port=" + KeycloakPassthroughIntegrationTest.PORT,
      "toolpool.mode=metatools",
      "toolpool.spec-location=" + ToolpoolFixtures.SAMPLE_SPEC,
      "toolpool.base-url=http://localhost:" + KeycloakPassthroughIntegrationTest.PORT,
      "toolpool.auth.mode=passthrough",
      "spring.security.oauth2.resourceserver.jwt.audiences=" + KeycloakTestRealm.TOOLPOOL_CLIENT_ID
    })
class KeycloakPassthroughIntegrationTest {

  static final int PORT = 18094;

  @DynamicPropertySource
  static void pointToolpoolAtKeycloak(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", KeycloakTestRealm::issuer);
  }

  @Test
  void callAToolThroughTheMetatoolsAsAUser_forwardsTheCallerTokenUnchanged() throws Exception {
    String callerToken = KeycloakTestRealm.userTokenFrom(KeycloakTestRealm.MCP_CLIENT_ID);

    Map<String, Object> answer = AuthTestMcpClient.callMetatoolGetUser(PORT, callerToken);

    assertThat(KeycloakTokenExchangeIntegrationTest.bearerTokenOf(answer)).isEqualTo(callerToken);
  }
}
