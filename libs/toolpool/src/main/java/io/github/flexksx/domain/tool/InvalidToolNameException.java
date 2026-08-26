package io.github.flexksx.domain.tool;

public class InvalidToolNameException extends IllegalArgumentException {

  public InvalidToolNameException(String message) {
    super(message);
  }
}
