package io.github.flexksx.toolpool.domain.tool;

public record ToolName(String value) {

  public ToolName {
    if (value.isBlank()) {
      throw new InvalidToolNameException("A tool name cannot be blank");
    }
  }
}
