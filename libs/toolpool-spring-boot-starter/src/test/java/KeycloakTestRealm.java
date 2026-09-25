import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

final class KeycloakTestRealm {

  static final String TOOLPOOL_CLIENT_ID = "toolpool";
  static final String TOOLPOOL_CLIENT_SECRET = "toolpool-secret";
  static final String MCP_CLIENT_ID = "mcp-client";
  static final String OTHER_CLIENT_ID = "other-client";
  static final String UPSTREAM_AUDIENCE = "upstream-api";

  private static final String IMAGE = "quay.io/keycloak/keycloak:26.7.4";
  private static final String REALM = "toolpool";
  private static final String USERNAME = "alice";
  private static final String PASSWORD = "alice-password";
  private static final int HTTP_PORT = 8080;
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final HttpClient HTTP = HttpClient.newHttpClient();

  private static GenericContainer<?> keycloak;

  private KeycloakTestRealm() {}

  static synchronized String issuer() {
    if (keycloak == null) {
      keycloak =
          new GenericContainer<>(IMAGE)
              .withCommand("start-dev", "--import-realm")
              .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
              .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
              .withCopyFileToContainer(
                  MountableFile.forClasspathResource("keycloak/toolpool-realm.json"),
                  "/opt/keycloak/data/import/toolpool-realm.json")
              .withExposedPorts(HTTP_PORT)
              .waitingFor(
                  Wait.forHttp("/realms/" + REALM)
                      .forPort(HTTP_PORT)
                      .withStartupTimeout(Duration.ofMinutes(3)));
      keycloak.start();
    }
    return "http://"
        + keycloak.getHost()
        + ":"
        + keycloak.getMappedPort(HTTP_PORT)
        + "/realms/"
        + REALM;
  }

  static String tokenUri() {
    return issuer() + "/protocol/openid-connect/token";
  }

  static String userTokenFrom(String clientId) throws IOException, InterruptedException {
    String form =
        Map.of(
                "grant_type", "password",
                "client_id", clientId,
                "username", USERNAME,
                "password", PASSWORD)
            .entrySet()
            .stream()
            .map(
                entry ->
                    entry.getKey()
                        + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    HttpResponse<String> response =
        HTTP.send(
            HttpRequest.newBuilder(URI.create(tokenUri()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("Keycloak refused the user login: " + response.body());
    }
    return JSON.readTree(response.body()).get("access_token").asString();
  }

  static Map<String, Object> claimsOf(String jwt) {
    String payload = jwt.split("\\.")[1];
    return JSON.readValue(Base64.getUrlDecoder().decode(payload), Map.class);
  }
}
