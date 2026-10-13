package io.github.flexksx.toolpool.application.port.out;

import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;

public interface SendToolCallPort {
  ToolCallResult sendToolCall(ToolCallRequest call);
}
