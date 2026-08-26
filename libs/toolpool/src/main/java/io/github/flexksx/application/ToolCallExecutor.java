package io.github.flexksx.application;

import io.github.flexksx.domain.tool.ToolCall;
import io.github.flexksx.domain.tool.ToolCallResult;

public interface ToolCallExecutor {
  ToolCallResult execute(ToolCall call);
}
