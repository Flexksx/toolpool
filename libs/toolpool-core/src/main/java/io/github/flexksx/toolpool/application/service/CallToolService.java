package io.github.flexksx.toolpool.application.service;

import io.github.flexksx.toolpool.application.port.in.CallToolUseCase;
import io.github.flexksx.toolpool.application.port.out.FindToolPort;
import io.github.flexksx.toolpool.application.port.out.ResolveUpstreamTokenPort;
import io.github.flexksx.toolpool.application.port.out.SendToolCallPort;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class CallToolService implements CallToolUseCase {

  private final FindToolPort findToolPort;
  private final SendToolCallPort sendToolCallPort;
  private final ResolveUpstreamTokenPort resolveUpstreamTokenPort;

  public CallToolService(FindToolPort findToolPort, SendToolCallPort sendToolCallPort) {
    this(findToolPort, sendToolCallPort, ResolveUpstreamTokenPort.none());
  }

  public CallToolService(
      FindToolPort findToolPort,
      SendToolCallPort sendToolCallPort,
      ResolveUpstreamTokenPort resolveUpstreamTokenPort) {
    this.findToolPort = findToolPort;
    this.sendToolCallPort = sendToolCallPort;
    this.resolveUpstreamTokenPort = resolveUpstreamTokenPort;
  }

  @Override
  public ToolCallResult callTool(
      ToolName name, @Nullable Map<String, Object> arguments, @Nullable AccessToken callerToken)
      throws ToolCatalogUnavailableException, UnknownToolException {
    return sendToolCallPort.sendToolCall(
        findToolPort
            .findTool(name)
            .requestFor(arguments)
            .withAccessToken(resolveUpstreamTokenPort.resolveUpstreamToken(callerToken)));
  }
}
