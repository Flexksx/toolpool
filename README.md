# jopenapi-mcp

Expose OpenAPI specs generated from your endpoints as MCP meta-tools via a simple and minimalist gateway.

## Stack

Java 25, Spring Boot 4.1, Spring AI 2.0 MCP server (WebMVC transport), built with Gradle.

## Getting started

```bash
direnv allow      # or: nix develop
just run          # starts the gateway on :8080
```

## Tasks

Every developer action goes through `just`. Run `just` for the full list.

| Command | Does |
| --- | --- |
| `just run` | Run the gateway locally |
| `just build all` | Build the executable jar |
| `just test all` | Run the test suite |
| `just format all` | Format Nix, Markdown and Java sources |
| `just lint all` | Check Markdown and Java formatting |

## Layout

Flat single-app repo — the gateway lives at the root, not under `apps/`.

- `src/` — gateway sources
- `nix/` — dev-shell modules (`devtools.nix`, `java.nix`)
- `.just/` — build/format/lint/test recipes
