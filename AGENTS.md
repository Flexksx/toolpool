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

The library is four modules. Each one is a separate jar, so a consumer takes only
the part it needs and no module drags a framework onto a consumer that did not
ask for it.

| Module | Holds | Third-party deps |
| --- | --- | --- |
| `libs/toolpool-core` | `domain`, `application` | `jspecify` |
| `libs/toolpool-openapi` | `adapter.openapi` | `swagger-parser`, `slf4j-api` |
| `libs/toolpool-mcp` | `adapter.mcp` | `io.modelcontextprotocol.sdk:mcp` |
| `libs/toolpool-spring-boot-starter` | `spring` | Spring Boot, Spring AI MCP |

- `libs/toolpool-core` is the whole model and every translation rule. It depends
  on the JDK and on `jspecify`. Nothing else.
- `libs/toolpool-openapi` and `libs/toolpool-mcp` depend on `toolpool-core` and
  on one framework-neutral third-party library each. Neither knows Spring.
- `libs/toolpool-spring-boot-starter` is the only module that knows Spring. It
  holds the composition root, the `RestClient` executor, the Spring AI meta-tool
  presentation and the `EnvironmentPostProcessor`.
- `apps/toolpool-demo` is the deployable demo app. It applies the
  `org.springframework.boot` plugin and depends on the starter.
- `tests/architecture` holds the cross-module architecture rules and no
  production code.

To support a second framework, add one module beside the starter. Implement
`ToolCallExecutor` with that framework's HTTP client and register the tools the
way that framework registers them. No other module changes.

## Architecture

`libs/toolpool-core` uses an onion architecture. Each ring is a package under
`io.github.flexksx.toolpool`. A ring must depend inward only.

- `domain`: the model and every translation rule. It depends on the JDK only.
  Sub-packages group the model by concept: `domain.tool` holds the `Tool`
  aggregate and `ToolCatalog`, `domain.http` holds `HttpTarget` and
  `ParameterLocation`, and `domain.schema` holds `JsonSchema`. The shared
  concepts (`domain.http` and `domain.schema`) must not depend on `domain.tool`.
- `application`: the `Toolpool` service and its two outbound ports,
  `ToolCatalogSource` and `ToolCallExecutor`. It depends on `domain` only.

The other three modules are adapters. Each one sits outside the onion.

- `adapter.openapi` is the anti-corruption layer.
  `OpenApiToolCatalogMapper` maps one `OpenAPI` object to one `ToolCatalog`, and
  `OpenApiToolCatalogSource` reads the spec and holds that catalog. This is the
  only package that can import `io.swagger`.
- `adapter.mcp` presents a catalog through the MCP Java SDK. `McpDirectTools`
  builds one `SyncToolSpecification` per tool. `ToolSummary` and `ToolDefinition`
  are the meta-tool response shapes, and `McpCallToolResults` maps one
  `ToolCallResult`.
- `spring` is the composition root plus the two Spring-only adapters.
  `ToolpoolAutoConfiguration` wires every ring together, so it can import any
  framework. `RestClientToolCallExecutor` sends one bound `ToolCall`.
  `McpGatewayMetatools` declares the three meta-tools with Spring AI annotations.

Put every rule that decides *what a tool is* in the domain. `Tool` owns its input
schema, its search match and `bind`, which turns raw arguments into a validated
`ToolCall`. A request body is one more `ToolParameter`, at
`ParameterLocation.BODY` and named `body`, so `inputSchema` and `bind` read one
list and MCP sees one flat argument map. `ToolName` owns the MCP naming rules, so
both modes expose one name for one operation. An adapter must make no such
decision.

`JsonSchema` owns the JSON Schema vocabulary. Every keyword literal (`type`,
`properties`, `required`, `description`) lives in that one class, and
`JsonSchema.objectOf` assembles an object schema. `Tool.inputSchema` collects the
parameter schemas and calls it. Do not spell a JSON Schema keyword anywhere else.

`OpenApiToolCatalogSource` reads the spec one time, on the first call, and keeps
the catalog for the life of the process. A spec change needs a restart. Add a
refresh interval only when a running instance must pick up a new spec.

`read_tool` answers the tool name, its description and its input schema. It does
not answer the raw OpenAPI operation, so the domain carries one schema shape and
both modes read it.

`Tool`, `ToolParameter`, `ToolCatalog` and `JsonSchema` are records that carry
behavior. That is correct. A record is a class whose identity is its components,
and a value object is supposed to answer questions about itself. Do not wrap one
in a plain class to move its methods somewhere else.

### The Spring lock-in that is left

`McpGatewayMetatools` declares the three meta-tools with the Spring AI `@McpTool`
annotation, so `metatools` mode is Spring-only today. A second framework must
declare those three methods again. To remove that, rebuild the meta-tools as
`SyncToolSpecification` values in `toolpool-mcp`, the way `McpDirectTools` builds
the direct tools. Then both modes run on the framework-neutral SDK.

## Architecture tests

`tests/architecture` is a Gradle subproject that holds no production code. Its
only job is `ToolpoolArchitectureTest`, which states the dependency graph of the
whole library with ArchUnit. It declares `testImplementation` on all four library
modules, so one file reads every module.

A cross-module test belongs in `tests/`, not in whichever module happens to see
every other module. Put the next one there too. A module's own `src/test` holds
the tests of that module only.

The test holds nine rules:

| Rule | States |
| --- | --- |
| `theDomainDependsOnNothingButItselfAndTheJdk` | the domain sees the JDK and `jspecify` |
| `theSharedDomainValuesDoNotKnowTheToolDomain` | `domain.http` and `domain.schema` do not see `domain.tool` |
| `theApplicationDependsOnNothingButTheDomainAndTheJdk` | the application sees the domain |
| `theOpenApiAdapterKnowsTheOnionAndSwaggerOnly` | the OpenAPI adapter adds Swagger and SLF4J and nothing else |
| `theMcpAdapterKnowsTheOnionAndTheMcpSdkOnly` | the MCP adapter adds the MCP SDK and nothing else |
| `onlyTheSpringModuleKnowsSpring` | no module outside `spring` imports Spring |
| `onlyTheOpenApiAdapterKnowsSwagger` | Swagger stays behind the OpenAPI adapter |
| `onlyTheMcpAdapterAndTheSpringModuleKnowTheMcpSdk` | the MCP SDK stays behind those two |
| `everyPackageDeclaresThatItIsNullMarked` | every package carries the nullness contract |

The last three rules run the negative direction of the two adapter allowlists.
Keep both directions. An allowlist says what one module can add, and the negative
rule says that no other module can add it.

The Gradle classpath already blocks most of this: `toolpool-core` cannot import
`io.swagger`, because `swagger-parser` is not on its compile classpath. These
rules state the intent behind that classpath, so a dependency added to the wrong
`build.gradle` fails a test instead of quietly widening a module. That is the
reason the rules survive the module split, and the reason
`theAdaptersDoNotDependOnEachOther` does not: the two adapter allowlists already
exclude each other.

Every module reaches this test's classpath as a jar, so the import must not carry
`ImportOption.DoNotIncludeJars`. With that option every rule reads zero classes
and fails with "failed to check any classes", which reads like a boundary
violation and is not one.

To confirm that a new rule works, break it on purpose one time and watch the test
fail. A boundary violation usually does not compile, because the module split
keeps the offending library off the classpath. So tighten the rule instead: drop
one entry from an allowlist, or point an exclusion at a package no class lives in,
and check that exactly one rule fails.

## Nullness

Every package declares `@NullMarked` in `package-info.java`. The
`everyPackageDeclaresThatItIsNullMarked` rule fails a package that does not, in
every module.

The main sources hold no `Objects.requireNonNull` call. Do not add one.
`OpenApiToolCatalogMapper` is the only class that builds a `Tool`, a
`ToolParameter` or an `HttpTarget`, so it is the one place that checks a value
from outside.

## Entry points

All developer actions go through `just`. Run `just --list --list-submodules` for the
current set.

Never invoke `gradle`, `moon`, `alejandra`, `rumdl`, `checkstyle`, or
`google-java-format` directly in docs or scripts. Add a recipe, so the hooks and
the task runner call the same command.

After you change code, run `just ci all`. That recipe builds and tests every
affected project. Use it instead of a manual `just test` on each module.

`just build toolpool`, `just test toolpool`, `just lint toolpool` and
`just format toolpool` each run one moon target, `#toolpool-lib:<task>`, which
fans the task out over every module tagged `toolpool-lib`. `just lint java` and
`just format java` run `#java:<task>` and cover every Java project, the demo app,
`sample-rest-api-client` and `tests/architecture` included. `just build all` and
`just test all` run `:<task>`, moon's every-project target.

`just test architecture` runs the cross-module rules alone. `just test all` and
`just ci all` include them.

Add a module and every recipe picks it up: `:<task>` reaches it as soon as
`.moon/workspace.yml` names it, and a `#<tag>:<task>` recipe reaches it as soon
as it carries that tag. Do not add a per-module recipe.

## Affected-project CI

`just ci all` runs `moon ci -g`. moon compares the working tree against git,
finds the projects that hold changed files, and runs the `build`, `test` and
`lint` tasks for those projects only. A fully cached run takes less than one
second.

The `-g` flag includes dependents. Without `-g`, a change in `libs/toolpool-core`
never tests `apps/toolpool-demo`, because moon examines only the projects that
hold changed files.

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

Every unit is registered in `.moon/workspace.yml` and routes *all* of its
`just build`/`just test`/`just lint`/`just format`/`just demo` recipes through
moon rather than calling `./gradlew` directly, so there is one invocation path
per unit, not two.

`.moon/tasks/java.yml` holds every shared task. A project's `moon.yml` carries
`language`, `layer`, `tags` and `dependsOn`, and declares a task only when that
task belongs to that one project. `apps/toolpool-demo/moon.yml` declares `run`
for that reason and nothing else.

moon merges *every* file under `.moon/tasks/` into *every* project. A
`tag-<tag>.yml` name does not scope the file, and neither does a
`<language>-<layer>.yml` name. Both were tried and both applied to all projects.
So one file holds the whole task set, and `-p $projectSource` stands in for the
Gradle path: it resolves to `libs/toolpool-core` or `apps/toolpool-demo`, which
`./gradlew -p` accepts. That way one task covers a `libs/` unit and an `apps/`
unit. Do not add a second file under `.moon/tasks/` and expect it to apply to a
subset.

Tags select projects on the command line, not task files. There are two:

- `java` marks every Java project. `moon run '#java:format'` formats them all.
- `toolpool-lib` marks the four library modules. `moon run '#toolpool-lib:build'`
  builds the library and leaves the demo app, `sample-rest-api-client` and
  `tests/architecture` alone.

A tag cannot inherit from another tag, so `toolpool-lib` does not imply `java`.
moon 2.x has no such feature. `constraints.tagRelationships` in
`.moon/workspace.yml` reads like one and is not: it restricts which tags a
project can *depend on*. A library module therefore spells out both tags. Give a
new project every tag that applies to it, or a `just` recipe misses it.

Task `deps` come from `dependsOn`. `libs/toolpool-openapi` declares
`dependsOn: [toolpool-core]`, and the shared `build` task declares
`deps: ['^:build']`, so moon builds `toolpool-core` first. Add the Gradle
`project(...)` dependency and the moon `dependsOn` entry together, or moon runs
the tasks in the wrong order.

`.moon/tasks/java.yml` declares `implicitInputs` for `settings.gradle`, `gradlew`,
`gradle/wrapper/**/*`, `gradle/java-conventions.gradle` and `config/**/*`. A task
input glob defaults to `**/*` inside the project directory, so no project sees the
root build files. Without `implicitInputs`, a change to `settings.gradle` or to
the shared convention script marks zero projects as affected, and `moon ci` runs
nothing and reports success.

The Gradle wrapper is pinned to the same version `nix/java.nix` provides. Bump both
together or Gradle downloads a second distribution.

## Conventions an agent can't derive from the code

- `CLAUDE.md` is a symlink to `AGENTS.md`. Edit `AGENTS.md`; never replace the symlink.
- `gradle/java-conventions.gradle` holds the settings every library module
  shares: the `java-library` and `pmd` plugins, the toolchain, the platform BOMs,
  `jspecify` and the test dependencies. Each module's `build.gradle` applies it
  and then declares its own dependencies and nothing else. Put a new shared
  setting in that script, not in four files. `apps/toolpool-demo` and
  `libs/sample-rest-api-client` do not apply it, because they apply the
  `org.springframework.boot` plugin and manage their own dependencies.
- Add `runInCI: skip` to every new moon task that does not exit, such as a server
  or a file watcher. `runInCI` defaults to `true`, so `moon ci` starts the task
  and hangs until the timeout. `apps/toolpool-demo/moon.yml` sets the value on
  its `run` task for that reason. The value does not block
  `moon run toolpool-demo:run`, so `just demo` still works.
- Two static analysis tools run, and they check different things. PMD runs inside
  Gradle, from `config/pmd/ruleset.xml`, and holds dead-code rules only: unused
  fields, methods, locals, parameters, assignments and imports. Checkstyle runs
  in the moon `lint` task, from `config/checkstyle/checkstyle.xml`, and holds
  `DeclarationOrder` and `UnusedImports`. Keep layout rules out of both files,
  because `google-java-format` owns layout. `DeclarationOrder` compares the
  access level of the static fields, so keep one access level for all of them in
  a class of constants.
- Java formatting is `google-java-format` from the dev shell, not a Gradle plugin:
  Gradle builds and tests, nothing else. Run `just format java`.
- Every moon `build` task calls the Gradle `assemble` task and every moon `test`
  task calls the Gradle `check` task, with `deps: ["~:build"]`. The Gradle `build`
  task runs `test`, so a moon `build` task and a moon `test` task ran the Gradle
  `test` task at the same time. The two processes then wrote the same files in
  `build/test-results/test/` and the run failed with `NoSuchFileException` or
  `EOFException`. `check` also carries the PMD tasks, so no project has to name
  them. Keep `assemble` in the `build` task, keep `check` in the `test` task, and
  keep the `~:build` dependency, or the race comes back.
- Nothing formats `*.gradle`. Hand-format those.
- Dependency versions come from the platform BOMs declared in
  `gradle/java-conventions.gradle`, which imports the Spring Boot BOM and the
  Spring AI BOM. Declare artifacts without a version unless the artifact is
  outside every BOM. Three are today: `swagger-parser`,
  `io.modelcontextprotocol.sdk:mcp` and `archunit-junit5`. The Spring AI BOM manages the Spring AI
  starters but not the MCP SDK the starters pull in, so that one carries an
  explicit version that must match the Spring AI version.
- `libs/toolpool-openapi` applies the `java-test-fixtures` plugin.
  `ToolpoolFixtures` and the sample specs live in `src/testFixtures`, because
  `toolpool-mcp` and the starter both build a catalog from a real spec in their
  tests. A module that needs them declares
  `testImplementation testFixtures(project(':libs:toolpool-openapi'))`.
- No module declares `maven-publish` yet. The BOMs are declared as `api
  platform(...)`, which is right for a workspace build and wrong for a published
  POM, because it would constrain a consumer's versions. Move them to
  `compileOnly`/`annotationProcessor` scope, or switch to explicit versions,
  before the first publish.
- `.gitignore` anchors `/build/` with a leading slash: an unanchored `build/` also
  matches `.just/build/` and silently untracks the build recipes.
- `/openapi/` holds generated specs and `.gitignore` ignores it. The
  `toolpool-demo` integration tests read
  `openapi/sample-rest-api-client.openapi.json`. If those tests fail on unexpected
  paths or parameter locations, the file is stale: run `just openapi all` to write
  it again from the running `sample-rest-api-client` app.
