package io.github.flexksx.toolpool.application;

import io.github.flexksx.toolpool.domain.tool.ToolCall;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;

public interface ToolCallExecutor {
  ToolCallResult execute(ToolCall call);
}
