import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.flexksx.adapter.http.RestClientToolCallExecutor;
import io.github.flexksx.domain.http.HttpMethod;
import io.github.flexksx.domain.http.HttpTarget;
import io.github.flexksx.domain.tool.ToolCall;
import io.github.flexksx.domain.tool.ToolCallResult;
import io.github.flexksx.domain.tool.ToolName;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class RestClientToolCallExecutorTest {

  private static final String BASE_URL = "http://api.test";
  private static final String USER_JSON = "{\"id\":\"u1\",\"name\":\"Ana\"}";
  private static final ToolName GET_USER = ToolName.of("getUser");

  private MockRestServiceServer apiServer;
  private RestClientToolCallExecutor executor;

  @BeforeEach
  void bindExecutorToAMockedApi() {
    RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
    apiServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    executor = new RestClientToolCallExecutor(restClientBuilder.build());
  }

  @Test
  void executeACallWithEveryParameterLocation_sendsEachArgumentToItsOwnLocation() {
    apiServer
        .expect(requestTo(BASE_URL + "/users/u1?verbose=true"))
        .andExpect(method(org.springframework.http.HttpMethod.GET))
        .andExpect(header("X-Request-Id", "r1"))
        .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));

    ToolCallResult result =
        executor.execute(
            new ToolCall(
                GET_USER,
                new HttpTarget(HttpMethod.GET, "/users/{id}"),
                Map.of("id", "u1"),
                Map.of("verbose", List.of("true")),
                Map.of("X-Request-Id", "r1"),
                Optional.empty()));

    apiServer.verify();
    assertThat(result).isEqualTo(new ToolCallResult(USER_JSON, false));
  }

  @Test
  void executeACallWithABody_sendsItAsJson() {
    apiServer
        .expect(requestTo(BASE_URL + "/users/u1"))
        .andExpect(method(org.springframework.http.HttpMethod.PUT))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(USER_JSON))
        .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));

    ToolCallResult result =
        executor.execute(
            new ToolCall(
                ToolName.of("updateUser"),
                new HttpTarget(HttpMethod.PUT, "/users/{id}"),
                Map.of("id", "u1"),
                Map.of(),
                Map.of(),
                Optional.of(orderedUserBody())));

    apiServer.verify();
    assertThat(result.failed()).isFalse();
  }

  @Test
  void executeACallThatAnswersAnErrorStatus_marksTheResultAsFailed() {
    apiServer
        .expect(requestTo(BASE_URL + "/users/missing"))
        .andRespond(
            withStatus(HttpStatus.NOT_FOUND).body("{\"detail\":\"User missing not found\"}"));

    ToolCallResult result = executor.execute(getUserCall("missing"));

    assertThat(result.failed()).isTrue();
    assertThat(result.content()).contains("User missing not found");
  }

  @Test
  void executeACallThatAnswersNoBody_reportsAnEmptyJsonObject() {
    apiServer.expect(requestTo(BASE_URL + "/users/u1")).andRespond(withSuccess());

    ToolCallResult result = executor.execute(getUserCall("u1"));

    assertThat(result.content()).isEqualTo("{}");
  }

  private static ToolCall getUserCall(String id) {
    return new ToolCall(
        GET_USER,
        new HttpTarget(HttpMethod.GET, "/users/{id}"),
        Map.of("id", id),
        Map.of(),
        Map.of(),
        Optional.empty());
  }

  private static Map<String, Object> orderedUserBody() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", "u1");
    body.put("name", "Ana");
    return body;
  }
}
