package io.github.flexksx.toolpool.domain.tool;

import java.util.regex.Pattern;

public record ToolName(String value) {

  public static final int MCP_TOOL_MAX_CHARACTERS_LENGTH = 64;

  private static final String MCP_TOOL_NAME_CHARACTERS = "a-zA-Z0-9._/-";

  private static final Pattern MCP_TOOL_NAME =
      Pattern.compile("[" + MCP_TOOL_NAME_CHARACTERS + "]+");
  private static final Pattern UNALLOWED_MCP_TOOL_NAME =
      Pattern.compile("[^" + MCP_TOOL_NAME_CHARACTERS + "]");
  private static final String UNALLOWED_MCP_TOOL_NAME_CHARACTER_REPLACEMENT = "_";

  public ToolName {
    if (value.isBlank()) {
      throw new InvalidToolNameException("A tool name cannot be blank");
    }
    if (value.length() > MCP_TOOL_MAX_CHARACTERS_LENGTH) {
      throw new InvalidToolNameException(
          "The tool name "
              + value
              + " is longer than "
              + MCP_TOOL_MAX_CHARACTERS_LENGTH
              + " characters");
    }
    if (!MCP_TOOL_NAME.matcher(value).matches()) {
      throw new InvalidToolNameException(
          "The tool name " + value + " holds characters other than a-z A-Z 0-9 _ . / -");
    }
  }

  public static ToolName of(String rawName) {
    String sanitized =
        UNALLOWED_MCP_TOOL_NAME
            .matcher(rawName)
            .replaceAll(UNALLOWED_MCP_TOOL_NAME_CHARACTER_REPLACEMENT);
    return new ToolName(
        sanitized.length() <= MCP_TOOL_MAX_CHARACTERS_LENGTH
            ? sanitized
            : sanitized.substring(0, MCP_TOOL_MAX_CHARACTERS_LENGTH));
  }
}
