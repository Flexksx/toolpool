# AGENTS.md

## Rules for agents

- Do not write to this file. Only Cristian edits it. If you find something that
  belongs here, report it in the chat and stop.
- `CLAUDE.md` is a symlink to `AGENTS.md`. Never replace the symlink.
- Run every developer action through `just`. Never call `gradle`, `moon`,
  `alejandra`, `rumdl`, `checkstyle` or `google-java-format` directly.
- After you change code, run `just ci all`.

## What toolpool does

`toolpool` reads an OpenAPI spec from another service. It re-exposes the
operations of that spec as MCP tools. The `toolpool.mode` property selects the
shape:

- `metatools` (default): three MCP meta-tools, `tool_search`, `read_tool` and
  `tool_call`.
- `direct`: one MCP tool for each OpenAPI operation.

## Modules

One Gradle multi-project build, rooted at `settings.gradle`. Every unit is a
subproject of that file. There is no nested `settings.gradle` and no per-unit
wrapper.

| Module | Holds | Third-party deps |
| --- | --- | --- |
| `libs/toolpool-core` | `domain`, `application` | `jspecify` |
| `libs/toolpool-openapi` | `adapter.openapi` | `swagger-parser`, `slf4j-api` |
| `libs/toolpool-mcp` | `adapter.mcp` | `io.modelcontextprotocol.sdk:mcp` |
| `libs/toolpool-spring-boot-starter` | `spring` | Spring Boot, Spring AI MCP |
| `apps/toolpool-demo` | the demo app | Spring Boot plugin, the starter |
| `libs/sample-rest-api-client` | a sample service with an OpenAPI spec | Spring Boot plugin |
| `tests/architecture` | the cross-module ArchUnit rules | `archunit-junit5` |

- `toolpool-core` depends on the JDK and on `jspecify`. Nothing else.
- `toolpool-openapi` and `toolpool-mcp` depend on `toolpool-core` and on one
  framework-neutral library each. Neither module knows Spring.
- `toolpool-spring-boot-starter` is the only module that knows Spring.
- `tests/architecture` holds no production code.

## Onion rings in toolpool-core

Each ring is a package under `io.github.flexksx.toolpool`. A ring depends
inward only.

- `domain`: the model and every translation rule. It depends on the JDK only.
  - `domain.tool`: the `Tool` aggregate, `ToolCatalog`, `ToolName`,
    `ToolParameter`, `ToolCall` and the domain exceptions.
  - `domain.http`: `HttpTarget`, `HttpMethod` and `ParameterLocation`.
  - `domain.schema`: `JsonSchema`.
  - `domain.http` and `domain.schema` do not depend on `domain.tool`.
- `application`: the `Toolpool` service and two outbound ports,
  `ToolCatalogSource` and `ToolCallExecutor`. It depends on `domain` only.

## Adapters

Each adapter sits outside the onion.

- `adapter.openapi` is the anti-corruption layer.
  `OpenApiToolCatalogMapper` maps one `OpenAPI` object to one `ToolCatalog`.
  `OpenApiToolCatalogSource` reads the spec and holds that catalog. It reads the
  spec one time, on the first call, and keeps it for the life of the process. A
  spec change needs a restart. This is the only package that can import
  `io.swagger`.
- `adapter.mcp` presents a catalog through the MCP Java SDK. `McpDirectTools`
  builds one `SyncToolSpecification` for each tool. `ToolSummary` and
  `ToolDefinition` are the meta-tool response shapes. `McpCallToolResults` maps
  one `ToolCallResult`.
- `spring` is the composition root plus two Spring-only adapters.
  `ToolpoolAutoConfiguration` wires every ring together.
  `RestClientToolCallExecutor` sends one bound `ToolCall`. `McpGatewayMetatools`
  declares the three meta-tools with Spring AI annotations, so `metatools` mode
  is Spring-only today.

## Where a rule lives

- Every rule that decides what a tool is lives in `domain`. An adapter makes no
  such decision.
- `Tool` owns its input schema, its search match and `bind`. `bind` turns raw
  arguments into a validated `ToolCall`.
- A request body is one more `ToolParameter`, at `ParameterLocation.BODY` and
  named `body`. `inputSchema` and `bind` read one list, and MCP sees one flat
  argument map.
- `ToolName` owns the MCP naming rules. Both modes expose one name for one
  operation.
- `JsonSchema` owns the JSON Schema vocabulary. Every keyword literal (`type`,
  `properties`, `required`, `description`) lives in that one class.
  `JsonSchema.objectOf` assembles an object schema. Do not spell a JSON Schema
  keyword anywhere else.
- `read_tool` answers the tool name, its description and its input schema. It
  does not answer the raw OpenAPI operation.

## Nullness

- Every package declares `@NullMarked` in `package-info.java`.
- The main sources hold no `Objects.requireNonNull` call. Do not add one.
- `OpenApiToolCatalogMapper` is the only class that builds a `Tool`, a
  `ToolParameter` or an `HttpTarget`. It is the one place that checks a value
  from outside.

## Architecture tests

`tests/architecture/src/test/java/ToolpoolArchitectureTest.java` states the
dependency graph with ArchUnit. It declares `testImplementation` on all four
library modules, so one file reads every module. Put the next cross-module test
in `tests/`. A module's own `src/test` holds the tests of that module only.

| Rule | States |
| --- | --- |
| `theDomainDependsOnNothingButItselfAndTheJdk` | the domain sees the JDK and `jspecify` |
| `theSharedDomainValuesDoNotKnowTheToolDomain` | `domain.http` and `domain.schema` do not see `domain.tool` |
| `theApplicationDependsOnNothingButTheDomainAndTheJdk` | the application sees the domain |
| `theOpenApiAdapterKnowsTheOnionAndSwaggerOnly` | the OpenAPI adapter adds Swagger and SLF4J only |
| `theMcpAdapterKnowsTheOnionAndTheMcpSdkOnly` | the MCP adapter adds the MCP SDK only |
| `onlyTheSpringModuleKnowsSpring` | no module outside `spring` imports Spring |
| `onlyTheOpenApiAdapterKnowsSwagger` | Swagger stays behind the OpenAPI adapter |
| `onlyTheMcpAdapterAndTheSpringModuleKnowTheMcpSdk` | the MCP SDK stays behind those two |
| `everyPackageDeclaresThatItIsNullMarked` | every package carries the nullness contract |

Every module reaches this classpath as a jar. The import must not carry
`ImportOption.DoNotIncludeJars`. With that option every rule reads zero classes
and fails with "failed to check any classes".

## Commands

Run `just --list --list-submodules` for the current set.

| Command | Runs |
| --- | --- |
| `just ci all` | the affected build, test and lint tasks, dependents included |
| `just ci staged` | the same tasks for the staged files only |
| `just build toolpool` | `#toolpool-lib:build`, the four library modules |
| `just test toolpool` | `#toolpool-lib:test` |
| `just lint java` | `#java:lint`, every Java project |
| `just format java` | `#java:format`, every Java project |
| `just build all` | `:build`, every project |
| `just test all` | `:test`, every project |
| `just test architecture` | the cross-module rules alone |
| `just demo all` | the demo app |
| `just demo stack` | `toolpool-demo` with `sample-rest-api-client` on port 18080 |
| `just openapi all` | writes the generated specs into `/openapi` |

A new module joins `:<task>` as soon as `.moon/workspace.yml` names it, and
joins a `#<tag>:<task>` recipe as soon as it carries that tag. Do not add a
per-module recipe.

## Build system

- `direnv allow` or `nix develop` loads the pinned toolchain from `flake.nix`
  and `nix/`. Nix owns every tool version. There is no `.prototools`.
- The Gradle wrapper is pinned to the version `nix/java.nix` provides. Bump both
  together, or Gradle downloads a second distribution.
- `gradle/java-conventions.gradle` holds the shared library settings: the
  `java-library` and `pmd` plugins, the toolchain, the platform BOMs, `jspecify`
  and the test dependencies. Put a new shared setting in that script.
  `apps/toolpool-demo` and `libs/sample-rest-api-client` do not apply it,
  because they apply the `org.springframework.boot` plugin.
- Dependency versions come from the Spring Boot BOM and the Spring AI BOM.
  Declare an artifact without a version unless the artifact is outside every
  BOM. Three are: `swagger-parser`, `io.modelcontextprotocol.sdk:mcp` and
  `archunit-junit5`. The MCP SDK version must match the Spring AI version.
- `libs/toolpool-openapi` applies the `java-test-fixtures` plugin.
  `ToolpoolFixtures` and the sample specs live in `src/testFixtures`. A module
  that needs them declares
  `testImplementation testFixtures(project(':libs:toolpool-openapi'))`.
- No module declares `maven-publish`.

### moon

- Every unit is registered in `.moon/workspace.yml`. Every `just` recipe routes
  through moon, not through `./gradlew`.
- `.moon/tasks/java.yml` holds every shared task. moon merges every file under
  `.moon/tasks/` into every project, so a `tag-<tag>.yml` or
  `<language>-<layer>.yml` name scopes nothing. Keep one file.
- `-p $projectSource` stands in for the Gradle path. It resolves to
  `libs/toolpool-core` or `apps/toolpool-demo`, which `./gradlew -p` accepts.
- A project's `moon.yml` carries `language`, `layer`, `tags` and `dependsOn`. It
  declares a task only when that task belongs to that one project.
- Two tags select projects on the command line. `java` marks every Java project.
  `toolpool-lib` marks the four library modules. A tag cannot inherit from
  another tag, so a library module spells out both. Give a new project every tag
  that applies to it.
- `constraints.tagRelationships` in `.moon/workspace.yml` restricts which tags a
  project can depend on. It is not tag inheritance.
- Task `deps` come from `dependsOn`. Add the Gradle `project(...)` dependency
  and the moon `dependsOn` entry together, or moon runs the tasks in the wrong
  order.
- `.moon/tasks/java.yml` declares `implicitInputs` for `settings.gradle`,
  `gradlew`, `gradle/wrapper/**/*`, `gradle/java-conventions.gradle` and
  `config/**/*`. A task input glob covers the project directory only, so without
  `implicitInputs` a change to a root build file marks zero projects as
  affected.
- Every moon `build` task calls the Gradle `assemble` task. Every moon `test`
  task calls the Gradle `check` task, with `deps: ["~:build"]`. The Gradle
  `build` task runs `test`, so `assemble` and `check` keep two processes off the
  same files in `build/test-results/test/`. `check` also carries the PMD tasks.
- Add `runInCI: skip` to every new moon task that does not exit, such as a
  server or a file watcher. `runInCI` defaults to `true`.

### Hooks

- `lefthook.yml` runs `just ci staged` in the `pre-commit` hook. Run
  `lefthook install` one time to write `.git/hooks/pre-commit`.
- The `lefthook.yml` commands declare `priority`. The three formatters run
  first and stage their fixes. The `ci` command runs last, at priority 4.
- `.claude/settings.json` runs `just ci all` in a `Stop` hook. The command exits
  with code 2 after a failure. It reads `stop_hook_active` and exits early on
  the second pass.

### Static analysis and formatting

- PMD runs inside Gradle, from `config/pmd/ruleset.xml`. It holds dead-code
  rules only: unused fields, methods, locals, parameters, assignments and
  imports.
- Checkstyle runs in the moon `lint` task, from
  `config/checkstyle/checkstyle.xml`. It holds `DeclarationOrder` and
  `UnusedImports`. `DeclarationOrder` compares the access level of the static
  fields, so keep one access level for all of them in a class of constants.
- `google-java-format` owns Java layout. It comes from the dev shell, not from a
  Gradle plugin. Keep layout rules out of the PMD and Checkstyle files.
- Nothing formats `*.gradle`. Hand-format those.

## Generated files

- `.gitignore` anchors `/build/` with a leading slash. An unanchored `build/`
  also matches `.just/build/` and untracks the build recipes.
- `/openapi/` holds generated specs and is ignored. The `toolpool-demo`
  integration tests read `openapi/sample-rest-api-client.openapi.json`. If those
  tests fail on unexpected paths or parameter locations, the file is stale. Run
  `just openapi all` to write it again from the running `sample-rest-api-client`
  app.
