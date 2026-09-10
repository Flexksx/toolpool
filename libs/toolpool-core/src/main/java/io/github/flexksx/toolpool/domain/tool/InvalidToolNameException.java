package io.github.flexksx.toolpool.domain.tool;

public class InvalidToolNameException extends IllegalArgumentException {

  public InvalidToolNameException(String message) {
    super(message);
  }
}
