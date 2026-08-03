# jopenapi-mcp

Expose OpenAPI specs generated from your endpoints as MCP meta-tools via a simple and minimalist gateway.

## Getting started

```bash
direnv allow      # or: nix develop
just              # list every task
```

## Tasks

Every developer action goes through `just`.

| Command | Does |
| --- | --- |
| `just format all` | Format Nix and Markdown sources |
| `just lint all` | Lint Markdown |

`just build` and `just test` are empty until the first unit lands.

## Layout

- `nix/` — dev-shell modules
- `.just/` — build/format/lint/test recipes

## Idea

If you have a REST API that has a ton of endpoints, there's no option of creating a MCP tool for each endpoint.

You may have the temptation to generate an MCP server based on an OpenAPI spec of your backend's endpoints,
but that is going to bloat the context of the agent in a few turns.

A different approach is that you use a dynamic tool router, that exposes your agent tools as "meta-tools".
In that case, the MCP would have only a handful of actual tools for the agent - `tool_call`, `tool_search`, `read_tool`.

Of course, it introduces a trade-off between latency, discovery and model context capabilities, 
but is an interesting approach in the case where you have a ton of REST endpoints and want an MCP for your agents.
