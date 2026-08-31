package io.github.flexksx.domain.tool;

import java.util.Locale;

public class MissingRequiredArgumentException extends IllegalArgumentException {

  public MissingRequiredArgumentException(ToolName toolName, ToolParameter parameter) {
    super(
        "Missing required "
            + parameter.location().name().toLowerCase(Locale.ROOT)
            + " argument "
            + parameter.name()
            + " for tool "
            + toolName.value());
  }
}
