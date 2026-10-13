package io.github.flexksx.toolpool.application.port.out;

import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;

public interface FindToolPort {
  Tool findTool(ToolName name) throws ToolCatalogUnavailableException, UnknownToolException;
}
