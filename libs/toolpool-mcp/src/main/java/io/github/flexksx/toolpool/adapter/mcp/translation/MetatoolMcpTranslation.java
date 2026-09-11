package io.github.flexksx.toolpool.adapter.mcp.translation;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;

public final class MetatoolMcpTranslation implements McpTranslation {

  private final List<SyncToolSpecification> specifications;

  public MetatoolMcpTranslation(List<SyncToolSpecification> specifications) {
    this.specifications = List.copyOf(specifications);
  }

  @Override
  public List<SyncToolSpecification> toolSpecifications() {
    return specifications;
  }
}
