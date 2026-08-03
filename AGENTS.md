# AGENTS.md

## Project map

`jopenapi-mcp` reads OpenAPI specs produced by other services and re-exposes their
operations as MCP tools.

No implementation exists yet — the repo currently holds only the toolchain and task
runner. There is no `apps/`, `libs/`, `openapi/`, or `infra/` directory, and none should
be created until it has a real occupant.

## Entry points

All developer actions go through `just`. Run `just --list --list-submodules` for the
current set.

- `just format all` — Nix and Markdown
- `just lint all` — Markdown

`just build` and `just test` are stubs. Add a recipe per unit as it lands. Never invoke
a build, format, or lint tool directly in docs or scripts — the pre-commit hooks and the
task runner must call the same command.

## Dev environment

`direnv allow` (or `nix develop`) loads the pinned toolchain from `flake.nix` + `nix/`.
Nix owns every tool version — there is no `.prototools`, and no `moon`, because there is
no dependency graph to resolve. Add `moon` + `proto` only once a second unit depends on
the first.

Add a `nix/<lang>.nix` the first time a unit needs that language, and put the language's
formatter and linter in the same file so they come from the dev shell, not from a build
plugin.

## Conventions an agent can't derive from the code

- `CLAUDE.md` is a symlink to this file. Edit `AGENTS.md`; never replace the symlink.
- Formatters and linters are dev-shell binaries invoked by `just`, not build-tool
  plugins. Keep build tools to building and testing.
