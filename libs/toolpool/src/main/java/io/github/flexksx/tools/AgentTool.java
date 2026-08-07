package io.github.flexksx.tools;

import java.util.Map;


public record AgentTool(
	String name,
	String description,
	Map<String, Object> inputSchema,
	Map<String, Object> outputSchema) {
}
