package io.github.flexksx.domain.tool;

public class MissingRequiredArgumentException extends IllegalArgumentException {

  public MissingRequiredArgumentException(ToolName toolName, ToolParameter parameter) {
    super(
        "Missing required "
            + parameter.location().name().toLowerCase(java.util.Locale.ROOT)
            + " parameter "
            + parameter.name()
            + " for tool "
            + toolName.value());
  }

  public MissingRequiredArgumentException(ToolName toolName, String argumentName) {
    super("Missing required " + argumentName + " argument for tool " + toolName.value());
  }
}
