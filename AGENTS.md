# AGENTS.md

## Project map

`toolpool` reads OpenAPI specs produced by other services and re-exposes their
operations as a handful of MCP meta-tools (`tool_search`, `read_tool`, `tool_call`)
rather than one MCP tool per endpoint.

One Gradle multi-project build, rooted at `settings.gradle`. Every unit is a
subproject included from there. There is no nested `settings.gradle` and no
per-unit wrapper.

- `libs/toolpool/`: OpenAPI parsing and the meta-tool router. Library only:
  no `@SpringBootApplication`, no `bootJar`. It depends on `spring-boot-autoconfigure`
  so a consuming app can pick it up, and on `swagger-parser` to read specs.
- `apps/toolpool-demo/`: the deployable demo app. Applies the
  `org.springframework.boot` plugin and depends on `project(':libs:toolpool')`.

## Entry points

All developer actions go through `just`. Run `just --list --list-submodules` for the
current set.

Never invoke `gradle`, `alejandra`, `rumdl`, or `google-java-format` directly in docs
or scripts. 
Add a recipe, so the pre-commit hooks and the task runner call the same
command.

After doing code modifications, make sure to run `just test` on the affected module.

## Dev environment

`direnv allow` (or `nix develop`) loads the pinned toolchain from `flake.nix` + `nix/`.
Nix owns every tool version. There is no `.prototools`.

Every unit registered in `.moon/workspace.yml` (currently `toolpool` and
`toolpool-demo`) routes *all* of its `just build`/`just test`/`just demo` recipes
through `moon run <project>:<task>` rather than calling `./gradlew` directly, so
there's one invocation path per unit, not two. `apps/toolpool-demo/moon.yml`
declares `deps: ['^:build']` on each task, which, combined with its `dependsOn:
[toolpool]`, makes moon build `libs:toolpool` before any `toolpool-demo`
task runs.

`sample-rest-api-client` stays on `./gradlew` directly: it isn't registered in
`.moon/workspace.yml` because nothing depends on it through moon. Register a
project in `.moon/workspace.yml`, and route its `just` recipes through
`moon run`, only once another unit's moon task actually depends on it. Don't
pre-wire `.moon/` for a unit with no cross-unit edge.

The Gradle wrapper is pinned to the same version `nix/java.nix` provides. Bump both
together or Gradle downloads a second distribution.

## Conventions an agent can't derive from the code

- `CLAUDE.md` is a symlink to `AGENTS.md`. Edit `AGENTS.md`; never replace the symlink.
- Java formatting is `google-java-format` from the dev shell, not a Gradle plugin:
  Gradle builds and tests, nothing else. Run `just format java`.
- Nothing formats `*.gradle`. Hand-format those.
- Dependency versions come from the platform BOMs declared in each `build.gradle`.
  Declare artifacts without a version unless the artifact is outside every BOM
  (`swagger-parser` is the one exception today).
- `.gitignore` anchors `/build/` with a leading slash: an unanchored `build/` also
  matches `.just/build/` and silently untracks the build recipes.
