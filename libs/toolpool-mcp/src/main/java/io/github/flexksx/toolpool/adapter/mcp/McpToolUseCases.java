package io.github.flexksx.toolpool.adapter.mcp;

import io.github.flexksx.toolpool.application.port.in.CallToolUseCase;
import io.github.flexksx.toolpool.application.port.in.DescribeToolUseCase;
import io.github.flexksx.toolpool.application.port.in.ListToolsUseCase;
import io.github.flexksx.toolpool.application.port.in.SearchToolsUseCase;

/** Holds the use cases that the MCP translation modes choose from. */
public record McpToolUseCases(
    ListToolsUseCase listToolsUseCase,
    SearchToolsUseCase searchToolsUseCase,
    DescribeToolUseCase describeToolUseCase,
    CallToolUseCase callToolUseCase) {}
