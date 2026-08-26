# AGENTS.md

## Project map

`toolpool` reads OpenAPI specs produced by other services and re-exposes their
operations as MCP tools. The `toolpool.mode` property selects how it exposes them:

- `metatools` (default): a handful of MCP meta-tools (`tool_search`, `read_tool`,
  `tool_call`) instead of one MCP tool per endpoint.
- `direct`: one MCP tool per OpenAPI operation.

One Gradle multi-project build, rooted at `settings.gradle`. Every unit is a
subproject included from there. There is no nested `settings.gradle` and no
per-unit wrapper.

- `libs/toolpool/`: OpenAPI parsing and the meta-tool router. Library only:
  no `@SpringBootApplication`, no `bootJar`. It depends on `spring-boot-autoconfigure`
  so a consuming app can pick it up, and on `swagger-parser` to read specs.
- `apps/toolpool-demo/`: the deployable demo app. Applies the
  `org.springframework.boot` plugin and depends on `project(':libs:toolpool')`.

## Architecture

`libs/toolpool` uses an onion architecture. Each ring is a package under
`io.github.flexksx`. A ring must depend inward only.

- `domain`: the model and every translation rule. It depends on the JDK only.
  Sub-packages group the model by concept: `domain.tool` holds the `Tool`
  aggregate and `ToolCatalog`, `domain.http` holds `HttpTarget` and
  `ParameterLocation`, and `domain.schema` holds `JsonSchema`. The shared
  concepts (`domain.http` and `domain.schema`) must not depend on `domain.tool`.
- `application`: the `Toolpool` service and its two outbound ports,
  `ToolCatalogProvider` and `ToolCallExecutor`. It depends on `domain` only.
- `adapter.openapi`: the anti-corruption layer.
  `OpenApiToolCatalogTranslator` maps one `OpenAPI` object to one `ToolCatalog`, and
  `OpenApiToolCatalogProvider` reads the spec and holds that catalog. This is the
  only package that can import `io.swagger`.
- `adapter.http`: `RestClientToolCallExecutor` sends one bound `ToolCall`.
- `adapter.mcp`: the two MCP presentations and the `EnvironmentPostProcessor`.
- `ToolpoolAutoConfiguration` is the composition root. It sits outside every ring
  and wires them together, so it can import any framework.

Put every rule that decides *what a tool is* in the domain. `Tool` owns its input
schema, its search match and `bind`, which turns raw arguments into a validated
`ToolCall`. `ToolName` owns the MCP naming rules, so both modes expose one name
for one operation. An adapter must make no such decision.

`OpenApiToolCatalogProvider` reads the spec one time, on the first call, and keeps
the catalog for the life of the process. A spec change needs a restart. Add a
refresh interval only when a running instance must pick up a new spec.

`read_tool` answers the tool name, its description and its input schema. It does
not answer the raw OpenAPI operation, so the domain carries one schema shape and
both modes read it.

`ToolpoolArchitectureTest` enforces the rules above with ArchUnit. If you add a
ring or a third-party library, add a rule. To confirm that a new rule works,
break it on purpose one time and watch the test fail.

## Nullness

Every package declares `@NullMarked` in `package-info.java`. The
`everyPackageDeclaresThatItIsNullMarked` rule fails a package that does not.

The main sources hold no `Objects.requireNonNull` call. Do not add one.
`OpenApiToolCatalogTranslator` is the only class that builds a `Tool`, a
`ToolParameter`, a `ToolBody` or an `HttpTarget`, so it is the one place that
checks a value from outside.

## Entry points

All developer actions go through `just`. Run `just --list --list-submodules` for the
current set.

Never invoke `gradle`, `moon`, `alejandra`, `rumdl`, or `google-java-format`
directly in docs or scripts. Add a recipe, so the hooks and the task runner call
the same command.

After you change code, run `just ci all`. That recipe builds and tests every
affected project. Use it instead of a manual `just test` on each module.

## Affected-project CI

`just ci all` runs `moon ci -g`. moon compares the working tree against git,
finds the projects that hold changed files, and runs the `build` and `test` tasks
for those projects only. A fully cached run takes less than one second.

The `-g` flag includes dependents. Without `-g`, a change in `libs/toolpool` never
tests `apps/toolpool-demo`, because moon examines only the projects that hold
changed files.

`just ci staged` pipes the staged file list into `moon ci --stdin -g`. The
pre-commit hook calls this recipe, so the hook tests the staged scope and not the
full dirty tree. Gradle still compiles the working tree. If you stage part of a
file, the test runs against the unstaged version of that file.

Two hooks call these recipes:

- `lefthook.yml` runs `just ci staged` in the `pre-commit` hook. Run
  `lefthook install` one time to write `.git/hooks/pre-commit`. Until you run
  that command, no entry in `lefthook.yml` has any effect.
- `.claude/settings.json` runs `just ci all` in a `Stop` hook. The command exits
  with code 2 after a failure, which returns the failure to the agent. The
  command also reads `stop_hook_active` and exits early on the second pass, so a
  broken test cannot start an infinite loop.

The `lefthook.yml` commands declare `priority`. The three formatters run first
and stage their fixes. The `ci` command runs last, at priority 4, so it tests the
formatted content. Without `priority`, lefthook sorts the commands by name and
`ci` runs before every formatter.

## Dev environment

`direnv allow` (or `nix develop`) loads the pinned toolchain from `flake.nix` + `nix/`.
Nix owns every tool version. There is no `.prototools`.

Every unit registered in `.moon/workspace.yml` (currently `toolpool` and
`toolpool-demo`) routes *all* of its `just build`/`just test`/`just demo` recipes
through `moon run <project>:<task>` rather than calling `./gradlew` directly, so
there is one invocation path per unit, not two. `apps/toolpool-demo/moon.yml`
declares `deps: ['^:build']` on each task, which, combined with its `dependsOn:
[toolpool]`, makes moon build `libs:toolpool` before any `toolpool-demo`
task runs.

`.moon/tasks/gradle.yml` holds the task configuration that every project
inherits. moon 2.x loads `.moon/tasks/**/*.yml`. It does not load
`.moon/tasks.yml`. If the file sits at the wrong path, moon logs
`Loaded 0 task configs for inheritance` and every setting in the file does
nothing.

That file declares `implicitInputs` for `settings.gradle`, `gradlew`, and
`gradle/wrapper/**/*`. A task input glob defaults to `**/*` inside the project
directory, so no project sees the root build files. Without `implicitInputs`, a
change to `settings.gradle` marks zero projects as affected, and `moon ci` runs
nothing and reports success.

`sample-rest-api-client` stays on `./gradlew` directly: it is not registered in
`.moon/workspace.yml` because nothing depends on it through moon. Register a
project in `.moon/workspace.yml`, and route its `just` recipes through
`moon run`, only once another unit's moon task actually depends on it. Do not
pre-wire `.moon/` for a unit with no cross-unit edge.

The Gradle wrapper is pinned to the same version `nix/java.nix` provides. Bump both
together or Gradle downloads a second distribution.

## Conventions an agent can't derive from the code

- `CLAUDE.md` is a symlink to `AGENTS.md`. Edit `AGENTS.md`; never replace the symlink.
- Add `runInCI: skip` to every new moon task that does not exit, such as a server
  or a file watcher. `runInCI` defaults to `true`, so `moon ci` starts the task
  and hangs until the timeout. `apps/toolpool-demo/moon.yml` sets the value on
  its `run` task for that reason. The value does not block
  `moon run toolpool-demo:run`, so `just demo` still works.
- `libs/toolpool` has a `lint` and a `format` moon task. `lint` checks
  `google-java-format` output, then runs `checkstyleMain` and `checkstyleTest`.
  `format` rewrites the sources, so it declares `runInCI: false` and `moon ci`
  skips it. Checkstyle holds `DeclarationOrder` and `UnusedImports`, in
  `libs/toolpool/config/checkstyle/checkstyle.xml`. Keep layout rules out of that
  file, because `google-java-format` owns layout. `DeclarationOrder` compares the
  access level of the static fields, so keep one access level for all of them in a
  class of constants.
- Java formatting is `google-java-format` from the dev shell, not a Gradle plugin:
  Gradle builds and tests, nothing else. Run `just format java`.
- Nothing formats `*.gradle`. Hand-format those.
- Dependency versions come from the platform BOMs declared in each `build.gradle`.
  Declare artifacts without a version unless the artifact is outside every BOM
  (`swagger-parser` and `archunit-junit5` are the two exceptions today). The
  Spring Boot BOM manages `org.jspecify:jspecify`, so it needs no version.
- `.gitignore` anchors `/build/` with a leading slash: an unanchored `build/` also
  matches `.just/build/` and silently untracks the build recipes.
- Every moon `build` task calls the Gradle `assemble` task, not `build`, and every
  moon `test` task declares `deps: ["~:build"]`. The Gradle `build` task runs
  `test`, so a moon `build` task and a moon `test` task ran the Gradle `test` task
  at the same time. The two processes then wrote the same files in
  `build/test-results/test/` and the run failed with `NoSuchFileException` or
  `EOFException`. Keep `assemble` in the `build` task, and keep the `~:build`
  dependency, or the race comes back.
- `/openapi/` holds generated specs and `.gitignore` ignores it. The
  `toolpool-demo` integration tests read
  `openapi/sample-rest-api-client.openapi.json`. If those tests fail on unexpected
  paths or parameter locations, the file is stale: run `just openapi all` to write
  it again from the running `sample-rest-api-client` app.
