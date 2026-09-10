package io.github.flexksx.toolpool.domain.tool;

public class UnknownToolException extends Exception {

  public UnknownToolException(ToolName toolName) {
    super("No tool named " + toolName.value());
  }
}
