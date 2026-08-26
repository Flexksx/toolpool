package io.github.flexksx.domain.tool;

import io.github.flexksx.domain.http.HttpTarget;

public class DuplicateToolNameException extends IllegalStateException {

  public DuplicateToolNameException(ToolName toolName, HttpTarget kept, HttpTarget rejected) {
    super(
        "Duplicate tool name "
            + toolName.value()
            + " on "
            + kept.describe()
            + " and "
            + rejected.describe());
  }
}
