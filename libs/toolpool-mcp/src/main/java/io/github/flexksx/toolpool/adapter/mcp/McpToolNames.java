package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.domain.tool.ToolName;
import java.util.regex.Pattern;

public final class McpToolNames {

  public static final int MAX_CHARACTERS_LENGTH = 64;

  private static final String ALLOWED_CHARACTERS = "a-zA-Z0-9._/-";
  private static final Pattern DISALLOWED_CHARACTER =
      Pattern.compile("[^" + ALLOWED_CHARACTERS + "]");
  private static final String DISALLOWED_CHARACTER_REPLACEMENT = "_";

  private McpToolNames() {}

  public static String publishedNameOf(ToolName name) {
    String sanitized =
        DISALLOWED_CHARACTER.matcher(name.value()).replaceAll(DISALLOWED_CHARACTER_REPLACEMENT);
    return sanitized.length() <= MAX_CHARACTERS_LENGTH
        ? sanitized
        : sanitized.substring(0, MAX_CHARACTERS_LENGTH);
  }
}
