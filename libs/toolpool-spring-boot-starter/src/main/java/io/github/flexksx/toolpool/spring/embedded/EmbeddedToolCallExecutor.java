package io.github.flexksx.toolpool.spring.embedded;

import io.github.flexksx.toolpool.application.port.out.SendToolCallPort;
import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.github.flexksx.toolpool.spring.RestClientToolCallExecutor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.client.RestClient;

final class EmbeddedToolCallExecutor implements SendToolCallPort {

  private final LocalServerAddress address;
  private final RestClient.Builder restClientBuilder;

  private @Nullable SendToolCallPort delegate;

  EmbeddedToolCallExecutor(LocalServerAddress address, RestClient.Builder restClientBuilder) {
    this.address = address;
    this.restClientBuilder = restClientBuilder;
  }

  @Override
  public ToolCallResult sendToolCall(ToolCallRequest call) {
    return delegate().sendToolCall(call);
  }

  private synchronized SendToolCallPort delegate() {
    if (delegate == null) {
      delegate =
          new RestClientToolCallExecutor(restClientBuilder.baseUrl(address.baseUrl()).build());
    }
    return delegate;
  }
}
