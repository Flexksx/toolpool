package io.github.flexksx.toolpool.application.port.in;

import io.github.flexksx.toolpool.application.port.out.ToolCatalogUnavailableException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;

public interface DescribeToolUseCase {
  Tool describeTool(ToolName name) throws ToolCatalogUnavailableException, UnknownToolException;
}
