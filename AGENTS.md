# AGENTS.md

## Project map

`jopenapi-mcp` is a single Spring Boot app, not a monorepo. The gateway lives at the
repo root — there is no `apps/`, `libs/`, `openapi/`, or `infra/` directory, and none
should be created until it has a real occupant.

It reads OpenAPI specs produced by other services and re-exposes their operations as
MCP tools over the Spring AI MCP server (WebMVC transport).

- `src/main/java/io/github/flexksx/jopenapimcp/` — gateway sources
- `src/main/resources/application.yaml` — MCP server name/version and HTTP port
- `nix/java.nix` — pins the JDK and Gradle for the dev shell

## Entry points

All developer actions go through `just`. Run `just --list --list-submodules` for the
current set.

- `just run` — run the gateway locally (`bootRun`)
- `just build all` — build the executable jar
- `just test all` — run the test suite
- `just format all` — Nix, Markdown, Java, Gradle
- `just lint all` — Markdown and Java/Gradle format checks

Never invoke `gradle`, `alejandra`, `rumdl`, or `spotless*` directly in docs or scripts.
Add a recipe instead, so the pre-commit hooks and the task runner stay in sync.

## Dev environment

`direnv allow` (or `nix develop`) loads the pinned toolchain from `flake.nix` + `nix/`.
Nix owns every tool version — there is no `.prototools`, and no `moon`, because a
single-app repo has no dependency graph to resolve. Add `moon` + `proto` only if a
second unit lands and depends on the first.

The Gradle wrapper is pinned to the same version the dev shell provides. Bump both
together (`nix/java.nix` and `gradle/wrapper/gradle-wrapper.properties`) or Gradle
will download a second distribution.

## Conventions an agent can't derive from the code

- `CLAUDE.md` is a symlink to this file. Edit `AGENTS.md`; never replace the symlink.
- `.gitignore` anchors `/build/` with a leading slash on purpose — an unanchored
  `build/` also matches `.just/build/` and silently untracks the build recipes.
- Java formatting is `google-java-format` (2-space indent) from the Nix dev shell, not
  a Gradle plugin — Gradle is for build and test only. Run `just format java`.
- Nothing formats `*.gradle.kts`. Hand-format those two files.
- Dependency versions come from the Spring Boot and Spring AI BOMs. Declare artifacts
  without a version unless the artifact is outside both BOMs.
