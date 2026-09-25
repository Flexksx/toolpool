import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.testing.ToolpoolFixtures;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = AuthTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
      "server.port=" + KeycloakTokenExchangeIntegrationTest.PORT,
      "toolpool.mode=direct",
      "toolpool.spec-location=" + ToolpoolFixtures.SAMPLE_SPEC,
      "toolpool.base-url=http://localhost:" + KeycloakTokenExchangeIntegrationTest.PORT,
      "toolpool.auth.mode=token-exchange",
      "toolpool.auth.token-exchange.client-id=" + KeycloakTestRealm.TOOLPOOL_CLIENT_ID,
      "toolpool.auth.token-exchange.client-secret=" + KeycloakTestRealm.TOOLPOOL_CLIENT_SECRET,
      "toolpool.auth.token-exchange.audience=" + KeycloakTestRealm.UPSTREAM_AUDIENCE,
      "spring.security.oauth2.resourceserver.jwt.audiences=" + KeycloakTestRealm.TOOLPOOL_CLIENT_ID
    })
class KeycloakTokenExchangeIntegrationTest {

  static final int PORT = 18095;
  private static final String METADATA_PATH = "/.well-known/oauth-protected-resource/mcp";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @DynamicPropertySource
  static void pointToolpoolAtKeycloak(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", KeycloakTestRealm::issuer);
    registry.add("toolpool.auth.token-exchange.token-uri", KeycloakTestRealm::tokenUri);
  }

  @Test
  void callAToolAsAUser_sendsAnExchangedTokenForTheSameUserToTheUpstreamApi() throws Exception {
    String callerToken = KeycloakTestRealm.userTokenFrom(KeycloakTestRealm.MCP_CLIENT_ID);

    Map<String, Object> answer = AuthTestMcpClient.callGetUser(PORT, callerToken);

    String upstreamToken = bearerTokenOf(answer);
    assertThat(upstreamToken).isNotEqualTo(callerToken);
    Map<String, Object> callerClaims = KeycloakTestRealm.claimsOf(callerToken);
    Map<String, Object> upstreamClaims = KeycloakTestRealm.claimsOf(upstreamToken);
    assertThat(upstreamClaims.get("sub")).isEqualTo(callerClaims.get("sub"));
    assertThat(upstreamClaims.get("azp")).isEqualTo(KeycloakTestRealm.TOOLPOOL_CLIENT_ID);
    assertThat(audienceOf(upstreamClaims)).contains(KeycloakTestRealm.UPSTREAM_AUDIENCE);
  }

  @Test
  void callWithoutAToken_answers401PointingAtTheResourceMetadata() throws Exception {
    HttpResponse<String> response = AuthTestMcpClient.initializeWith(PORT, null);

    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE))
        .hasValueSatisfying(
            header ->
                assertThat(header)
                    .startsWith("Bearer")
                    .contains(
                        "resource_metadata=\""
                            + AuthTestMcpClient.baseUrlOf(PORT)
                            + METADATA_PATH
                            + "\""));
  }

  @Test
  void callWithAForgedToken_answers401() throws Exception {
    HttpResponse<String> response =
        AuthTestMcpClient.initializeWith(PORT, "Bearer not-a-real-token");

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void callWithATokenIssuedForAnotherAudience_answers401() throws Exception {
    String foreignToken = KeycloakTestRealm.userTokenFrom(KeycloakTestRealm.OTHER_CLIENT_ID);

    HttpResponse<String> response =
        AuthTestMcpClient.initializeWith(PORT, "Bearer " + foreignToken);

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void readTheResourceMetadata_namesTheMcpEndpointAndTheAuthorizationServer() throws Exception {
    HttpResponse<String> response = AuthTestMcpClient.get(PORT, METADATA_PATH);

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode metadata = JSON.readTree(response.body());
    assertThat(metadata.get("resource").asString())
        .isEqualTo(AuthTestMcpClient.baseUrlOf(PORT) + AuthTestMcpClient.MCP_ENDPOINT);
    assertThat(metadata.get("authorization_servers").get(0).asString())
        .isEqualTo(KeycloakTestRealm.issuer());
  }

  static String bearerTokenOf(Map<String, Object> answer) {
    String authorization = String.valueOf(answer.get("authorization"));
    assertThat(authorization).startsWith("Bearer ");
    return authorization.substring("Bearer ".length());
  }

  private static List<Object> audienceOf(Map<String, Object> claims) {
    Object audience = claims.get("aud");
    return audience instanceof List<?> many ? List.copyOf(many) : List.of(String.valueOf(audience));
  }
}
