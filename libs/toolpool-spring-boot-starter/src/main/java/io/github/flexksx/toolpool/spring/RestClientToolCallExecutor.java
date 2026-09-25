package io.github.flexksx.toolpool.spring;

import io.github.flexksx.toolpool.application.ToolCallExecutor;
import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RestClientToolCallExecutor implements ToolCallExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestClientToolCallExecutor.class);
  private static final String EMPTY_RESPONSE_BODY = "{}";
  private static final String ABSENT_BODY = "none";
  private static final String PRESENT_BODY = "present";

  private final RestClient restClient;

  public RestClientToolCallExecutor(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public ToolCallResult execute(ToolCallRequest call) {
    long startedAtNanos = System.nanoTime();
    LOGGER
        .atDebug()
        .setMessage("Tool {} sends {} with path {}, query {}, header names {} and body {}")
        .addArgument(call.toolName().value())
        .addArgument(call.target().describe())
        .addArgument(call.pathVariables())
        .addArgument(call.queryParameters())
        .addArgument(call.headers().keySet())
        .addArgument(call.body() == null ? ABSENT_BODY : PRESENT_BODY)
        .log();

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

    ResponseEntity<String> response;
    try {
      response =
          request.retrieve().onStatus(status -> true, (req, res) -> {}).toEntity(String.class);
    } catch (RestClientException failure) {
      return new ToolCallResult(failureMessageOf(failure), true);
    }

    LOGGER
        .atInfo()
        .setMessage("Tool {} sent {} and read status {} in {} ms")
        .addArgument(call.toolName().value())
        .addArgument(call.target().describe())
        .addArgument(response.getStatusCode().value())
        .addArgument(Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis())
        .addKeyValue("tool", call.toolName().value())
        .addKeyValue("status", response.getStatusCode().value())
        .log();

    return new ToolCallResult(
        response.getBody() == null ? EMPTY_RESPONSE_BODY : response.getBody(),
        response.getStatusCode().isError());
  }

  private static String failureMessageOf(RestClientException failure) {
    return failure.getMessage() == null ? failure.toString() : failure.getMessage();
  }

  private static MultiValueMap<String, String> queryParametersOf(ToolCallRequest call) {
    MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
    call.queryParameters().forEach(queryParameters::addAll);
    return queryParameters;
  }
}
