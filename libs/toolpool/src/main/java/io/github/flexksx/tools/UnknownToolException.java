package io.github.flexksx.tools;

public class UnknownToolException extends Exception {

  public UnknownToolException(String toolName) {
    super("No tool named " + toolName);
  }
}
