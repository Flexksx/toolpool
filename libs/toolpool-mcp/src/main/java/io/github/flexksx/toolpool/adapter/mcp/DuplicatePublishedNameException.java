package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.domain.tool.ToolName;

public class DuplicatePublishedNameException extends IllegalStateException {

  public DuplicatePublishedNameException(String publishedName, ToolName kept, ToolName rejected) {
    super(
        "Tools "
            + kept.value()
            + " and "
            + rejected.value()
            + " both publish to the MCP name "
            + publishedName);
  }
}
