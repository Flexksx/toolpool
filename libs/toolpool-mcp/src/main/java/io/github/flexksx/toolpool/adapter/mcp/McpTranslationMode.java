package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.adapter.mcp.metatool.McpMetatool;
import io.github.flexksx.toolpool.adapter.mcp.translation.DirectMcpTranslation;
import io.github.flexksx.toolpool.adapter.mcp.translation.McpTranslation;
import io.github.flexksx.toolpool.adapter.mcp.translation.MetatoolMcpTranslation;
import java.util.function.Function;

public enum McpTranslationMode {
  METATOOLS(
      useCases ->
          new MetatoolMcpTranslation(
              McpMetatool.specificationsOf(
                  useCases.searchToolsUseCase(),
                  useCases.describeToolUseCase(),
                  useCases.callToolUseCase()))),

  DIRECT(
      useCases ->
          new DirectMcpTranslation(useCases.listToolsUseCase(), useCases.callToolUseCase()));

  private final Function<McpToolUseCases, McpTranslation> translation;

  McpTranslationMode(Function<McpToolUseCases, McpTranslation> translation) {
    this.translation = translation;
  }

  public McpTranslation translationOf(McpToolUseCases useCases) {
    return translation.apply(useCases);
  }
}
