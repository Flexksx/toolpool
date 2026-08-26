package io.github.flexksx.domain.tool;

import java.util.regex.Pattern;

public record ToolName(String value) {

  public static final int MAX_LENGTH = 64;

  private static final Pattern UNSUPPORTED_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_-]");
  private static final String REPLACEMENT = "_";

  public ToolName {
    if (value.isBlank()) {
      throw new InvalidToolNameException("A tool name cannot be blank");
    }
    if (value.length() > MAX_LENGTH) {
      throw new InvalidToolNameException(
          "The tool name " + value + " is longer than " + MAX_LENGTH + " characters");
    }
    if (UNSUPPORTED_CHARACTERS.matcher(value).find()) {
      throw new InvalidToolNameException(
          "The tool name " + value + " holds characters other than letters, digits, _ and -");
    }
  }

  public static ToolName of(String rawName) {
    String sanitized = UNSUPPORTED_CHARACTERS.matcher(rawName).replaceAll(REPLACEMENT);
    return new ToolName(
        sanitized.length() <= MAX_LENGTH ? sanitized : sanitized.substring(0, MAX_LENGTH));
  }

  @Override
  public String toString() {
    return value;
  }
}
