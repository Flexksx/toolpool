# AGENTS.md

## Project map

`jopenapi-mcp` reads OpenAPI specs produced by other services and re-exposes their
operations as a handful of MCP meta-tools (`tool_search`, `read_tool`, `tool_call`)
rather than one MCP tool per endpoint.

One Gradle multi-project build, rooted at `settings.gradle`. Every unit is a
subproject included from there — there is no nested `settings.gradle` and no
per-unit wrapper.

- `libs/jopenapimcp/` — OpenAPI parsing and the meta-tool router. Library only:
  no `@SpringBootApplication`, no `bootJar`. It depends on `spring-boot-autoconfigure`
  so a consuming app can pick it up, and on `swagger-parser` to read specs.

There is no deployable app yet. When one lands it goes in `apps/`, applies the
`org.springframework.boot` plugin, and depends on `project(':libs:jopenapimcp')`.

## Entry points

All developer actions go through `just`. Run `just --list --list-submodules` for the
current set.

- `just build all` / `just build jopenapimcp`
- `just test all` / `just test jopenapimcp`
- `just format all` — Nix, Markdown, Java
- `just lint all` — Markdown and Java format checks

Never invoke `gradle`, `alejandra`, `rumdl`, or `google-java-format` directly in docs
or scripts. Add a recipe, so the pre-commit hooks and the task runner call the same
command.

## Dev environment

`direnv allow` (or `nix develop`) loads the pinned toolchain from `flake.nix` + `nix/`.
Nix owns every tool version — there is no `.prototools`, and no `moon`: Gradle already
resolves the dependency graph between subprojects.

The Gradle wrapper is pinned to the same version `nix/java.nix` provides. Bump both
together or Gradle downloads a second distribution.

## Conventions an agent can't derive from the code

- `CLAUDE.md` is a symlink to `AGENTS.md`. Edit `AGENTS.md`; never replace the symlink.
- Java formatting is `google-java-format` from the dev shell, not a Gradle plugin —
  Gradle builds and tests, nothing else. Run `just format java`.
- Nothing formats `*.gradle`. Hand-format those.
- Dependency versions come from the platform BOMs declared in each `build.gradle`.
  Declare artifacts without a version unless the artifact is outside every BOM
  (`swagger-parser` is the one exception today).
- `.gitignore` anchors `/build/` with a leading slash: an unanchored `build/` also
  matches `.just/build/` and silently untracks the build recipes.
