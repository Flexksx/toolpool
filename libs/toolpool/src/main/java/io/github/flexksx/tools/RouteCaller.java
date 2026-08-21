package io.github.flexksx.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public class RouteCaller {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteCaller.class);
  private static final String EMPTY_RESPONSE_BODY = "{}";

  private final RestClient restClient;

  public RouteCaller(RestClient restClient) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
  }

  public CallToolResult call(HttpRoute route, Map<String, Object> arguments) {
    BoundArguments bound = bind(route, arguments);

    RestClient.RequestBodySpec request =
        restClient
            .method(HttpMethod.valueOf(route.method().name()))
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path(route.path())
                        .queryParams(bound.queryParameters())
                        .build(bound.pathVariables()))
            .headers(httpHeaders -> bound.headers().forEach(httpHeaders::add));

    if (bound.body() != null) {
      request = request.contentType(MediaType.APPLICATION_JSON).body(bound.body());
    }

    ResponseEntity<String> response =
        request.retrieve().onStatus(status -> true, (req, res) -> {}).toEntity(String.class);

    return CallToolResult.builder()
        .addTextContent(response.getBody() == null ? EMPTY_RESPONSE_BODY : response.getBody())
        .isError(response.getStatusCode().isError())
        .build();
  }

  private static BoundArguments bind(HttpRoute route, Map<String, Object> arguments) {
    Map<String, Object> given = arguments == null ? Map.of() : arguments;
    Map<String, Object> pathVariables = new LinkedHashMap<>();
    MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
    Map<String, String> headers = new LinkedHashMap<>();

    for (ToolParameter parameter : route.parameters()) {
      Object value = given.get(parameter.name());
      if (value == null) {
        rejectMissingRequiredParameter(route, parameter);
        continue;
      }
      switch (parameter.location()) {
        case PATH -> pathVariables.put(parameter.name(), value);
        case QUERY -> queryParameters.add(parameter.name(), String.valueOf(value));
        case HEADER -> headers.put(parameter.name(), String.valueOf(value));
        case COOKIE ->
            LOGGER.warn(
                "Ignored the cookie parameter {} of {} {}",
                parameter.name(),
                route.method(),
                route.path());
      }
    }
    return new BoundArguments(
        pathVariables, queryParameters, headers, given.get(ToolInputSchema.BODY_PROPERTY));
  }

  private static void rejectMissingRequiredParameter(HttpRoute route, ToolParameter parameter) {
    if (parameter.required()) {
      throw new IllegalArgumentException(
          "Missing required "
              + parameter.location().name().toLowerCase(java.util.Locale.ROOT)
              + " parameter "
              + parameter.name()
              + " for tool "
              + route.toolName());
    }
  }

  private record BoundArguments(
      Map<String, Object> pathVariables,
      MultiValueMap<String, String> queryParameters,
      Map<String, String> headers,
      Object body) {}
}
