package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.application.ToolCallExecutor;
import io.github.flexksx.toolpool.domain.tool.ToolCall;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public class RestClientToolCallExecutor implements ToolCallExecutor {

  private static final String EMPTY_RESPONSE_BODY = "{}";

  private final RestClient restClient;

  public RestClientToolCallExecutor(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public ToolCallResult execute(ToolCall call) {
    RestClient.RequestBodySpec request =
        restClient
            .method(HttpMethod.valueOf(call.target().method().name()))
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path(call.target().path())
                        .queryParams(queryParametersOf(call))
                        .build(call.pathVariables()))
            .headers(httpHeaders -> call.headers().forEach(httpHeaders::add));

    Object body = call.body();
    if (body != null) {
      request = request.contentType(MediaType.APPLICATION_JSON).body(body);
    }

    ResponseEntity<String> response =
        request.retrieve().onStatus(status -> true, (req, res) -> {}).toEntity(String.class);

    return new ToolCallResult(
        response.getBody() == null ? EMPTY_RESPONSE_BODY : response.getBody(),
        response.getStatusCode().isError());
  }

  private static MultiValueMap<String, String> queryParametersOf(ToolCall call) {
    MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
    call.queryParameters().forEach(queryParameters::addAll);
    return queryParameters;
  }
}
