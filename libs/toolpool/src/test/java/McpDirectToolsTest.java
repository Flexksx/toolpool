import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.adapter.mcp.McpDirectTools;
import io.github.flexksx.application.ToolCatalogProvider;
import io.github.flexksx.application.Toolpool;
import io.github.flexksx.domain.http.HttpMethod;
import io.github.flexksx.domain.http.HttpTarget;
import io.github.flexksx.domain.http.ParameterLocation;
import io.github.flexksx.domain.schema.JsonSchema;
import io.github.flexksx.domain.tool.Tool;
import io.github.flexksx.domain.tool.ToolCallResult;
import io.github.flexksx.domain.tool.ToolCatalog;
import io.github.flexksx.domain.tool.ToolDocumentation;
import io.github.flexksx.domain.tool.ToolName;
import io.github.flexksx.domain.tool.ToolParameter;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class McpDirectToolsTest {

  private static final ToolCallResult OK = new ToolCallResult("{}", false);

  private final ToolpoolFixtures.RecordingCallExecutor callExecutor =
      new ToolpoolFixtures.RecordingCallExecutor(OK);

  @Test
  void toolSpecifications_registersOneMcpToolPerOperationWithADescribedInputSchema()
      throws Exception {
    var specifications = directToolsFor(ToolpoolFixtures.SAMPLE_SPEC).toolSpecifications();

    assertThat(specifications)
        .extracting(SyncToolSpecification::tool)
        .extracting(McpSchema.Tool::name)
        .containsExactlyInAnyOrder("getUser", "createUser", "updateUser");
    assertThat(specifications)
        .extracting(SyncToolSpecification::tool)
        .allSatisfy(
            tool -> {
              assertThat(tool.description()).isNotBlank();
              assertThat(tool.inputSchema()).containsEntry("type", "object");
            });
  }

  @Test
  void toolSpecifications_exposesTheSanitizedToolName() throws Exception {
    McpDirectTools directTools = directToolsOver(toolNamed("get /users/{id}", null, null));

    assertThat(directTools.toolSpecifications())
        .extracting(specification -> specification.tool().name())
        .containsExactly("get_/users/_id_");
  }

  @Test
  void toolSpecifications_fallsBackToTheMethodAndThePathAsDescription() throws Exception {
    McpDirectTools directTools = directToolsOver(toolNamed("listUsers", null, null));

    assertThat(directTools.toolSpecifications())
        .extracting(specification -> specification.tool().description())
        .containsExactly("GET /users");
  }

  @Test
  void toolSpecifications_carriesTheSummaryAsTheMcpTitle() throws Exception {
    McpDirectTools directTools =
        directToolsOver(toolNamed("listUsers", "List users", "Answers every stored user"));

    assertThat(directTools.toolSpecifications())
        .extracting(specification -> specification.tool().title())
        .containsExactly("List users");
  }

  @Test
  void callAToolWithoutARequiredArgument_answersAnErrorResultInsteadOfThrowing() throws Exception {
    McpDirectTools directTools = directToolsOver(toolRequiringAPathId());

    McpSchema.CallToolResult result = callFirstTool(directTools, Map.of());

    assertThat(result.isError()).isTrue();
    assertThat(textOf(result)).contains("id");
    assertThat(callExecutor.calls()).isEmpty();
  }

  @Test
  void callAToolWithItsArguments_passesTheBoundCallToTheExecutor() throws Exception {
    McpDirectTools directTools = directToolsOver(toolRequiringAPathId());

    McpSchema.CallToolResult result = callFirstTool(directTools, Map.of("id", "u1"));

    assertThat(result.isError()).isFalse();
    assertThat(callExecutor.onlyCall().pathVariables()).containsExactly(Map.entry("id", "u1"));
  }

  private McpDirectTools directToolsFor(String specLocation) {
    return new McpDirectTools(
        new Toolpool(ToolpoolFixtures.catalogProviderFor(specLocation), callExecutor));
  }

  private McpDirectTools directToolsOver(Tool tool) {
    ToolCatalog catalog = ToolCatalog.of(List.of(tool));
    ToolCatalogProvider provider = () -> catalog;
    return new McpDirectTools(new Toolpool(provider, callExecutor));
  }

  private static McpSchema.CallToolResult callFirstTool(
      McpDirectTools directTools, Map<String, Object> arguments) throws Exception {
    SyncToolSpecification specification = directTools.toolSpecifications().getFirst();
    return specification
        .callHandler()
        .apply(null, new McpSchema.CallToolRequest(specification.tool().name(), arguments, null));
  }

  private static Tool toolNamed(String rawName, String summary, String description) {
    return new Tool(
        ToolName.of(rawName),
        new HttpTarget(HttpMethod.GET, "/users"),
        new ToolDocumentation(summary, description, List.of()),
        List.of(),
        null);
  }

  private static Tool toolRequiringAPathId() {
    return new Tool(
        ToolName.of("getUser"),
        new HttpTarget(HttpMethod.GET, "/users/{id}"),
        new ToolDocumentation("Get a user", null, List.of()),
        List.of(
            new ToolParameter(
                "id",
                ParameterLocation.PATH,
                true,
                new JsonSchema(Map.of("type", "string")),
                null)),
        null);
  }

  private static String textOf(McpSchema.CallToolResult result) {
    return ((McpSchema.TextContent) result.content().getFirst()).text();
  }
}
