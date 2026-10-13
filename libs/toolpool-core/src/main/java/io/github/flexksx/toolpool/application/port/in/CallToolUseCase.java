package io.github.flexksx.toolpool.application.port.in;

import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface CallToolUseCase {
  ToolCallResult callTool(
      ToolName name, @Nullable Map<String, Object> arguments, @Nullable AccessToken callerToken)
      throws ToolCatalogUnavailableException, UnknownToolException;
}
