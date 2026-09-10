package io.github.flexksx.toolpool.application;

import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;

public interface ToolCallExecutor {
  ToolCallResult execute(ToolCallRequest call);
}
