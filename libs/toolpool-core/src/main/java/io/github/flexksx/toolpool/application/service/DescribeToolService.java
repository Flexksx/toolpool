package io.github.flexksx.toolpool.application.service;

import io.github.flexksx.toolpool.application.port.in.DescribeToolUseCase;
import io.github.flexksx.toolpool.application.port.out.FindToolPort;
import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;

public class DescribeToolService implements DescribeToolUseCase {

  private final FindToolPort findToolPort;

  public DescribeToolService(FindToolPort findToolPort) {
    this.findToolPort = findToolPort;
  }

  @Override
  public Tool describeTool(ToolName name)
      throws ToolCatalogUnavailableException, UnknownToolException {
    return findToolPort.findTool(name);
  }
}
