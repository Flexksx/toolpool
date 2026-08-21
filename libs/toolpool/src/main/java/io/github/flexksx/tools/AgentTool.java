package io.github.flexksx.tools;

import com.github.fge.jsonschema.main.JsonSchema;

public record AgentTool(
    String name, String description, JsonSchema inputSchema, JsonSchema outputSchema) {}
