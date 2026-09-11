package io.github.flexksx.toolpool.adapter.mcp.translation;

import io.github.flexksx.toolpool.application.ToolCatalogUnavailableException;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;

public sealed interface McpTranslation permits MetatoolMcpTranslation, DirectMcpTranslation {

  List<SyncToolSpecification> toolSpecifications() throws ToolCatalogUnavailableException;
}
