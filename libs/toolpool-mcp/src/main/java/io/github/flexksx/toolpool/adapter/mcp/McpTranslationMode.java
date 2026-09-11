package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.adapter.mcp.metatool.McpMetatool;
import io.github.flexksx.toolpool.adapter.mcp.translation.DirectMcpTranslation;
import io.github.flexksx.toolpool.adapter.mcp.translation.McpTranslation;
import io.github.flexksx.toolpool.adapter.mcp.translation.MetatoolMcpTranslation;
import io.github.flexksx.toolpool.application.Toolpool;
import java.util.function.Function;

public enum McpTranslationMode {
  METATOOLS(toolpool -> new MetatoolMcpTranslation(McpMetatool.specificationsOf(toolpool))),

  DIRECT(DirectMcpTranslation::new);

  private final Function<Toolpool, McpTranslation> translation;

  McpTranslationMode(Function<Toolpool, McpTranslation> translation) {
    this.translation = translation;
  }

  public McpTranslation translationOf(Toolpool toolpool) {
    return translation.apply(toolpool);
  }
}
