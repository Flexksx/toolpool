import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.adapter.mcp.DuplicatePublishedNameException;
import io.github.flexksx.toolpool.adapter.mcp.translation.DirectMcpTranslation;
import io.github.flexksx.toolpool.application.ToolCatalogSource;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.http.HttpMethod;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.http.ParameterLocation;
import io.github.flexksx.toolpool.domain.schema.JsonSchema;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import io.github.flexksx.toolpool.domain.tool.ToolDocumentation;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import io.github.flexksx.toolpool.testing.ToolpoolFixtures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class DirectMcpTranslationTest {

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
    DirectMcpTranslation directMcpTranslation =
        directToolsOver(toolNamed("get /users/{id}", null, null));

    assertThat(directMcpTranslation.toolSpecifications())
        .extracting(specification -> specification.tool().name())
        .containsExactly("get_/users/_id_");
  }

  @Test
  void toolSpecifications_fallsBackToTheMethodAndThePathAsDescription() throws Exception {
    DirectMcpTranslation directMcpTranslation = directToolsOver(toolNamed("listUsers", null, null));

    assertThat(directMcpTranslation.toolSpecifications())
        .extracting(specification -> specification.tool().description())
        .containsExactly("GET /users");
  }

  @Test
  void toolSpecifications_carriesTheSummaryAsTheMcpTitle() throws Exception {
    DirectMcpTranslation directMcpTranslation =
        directToolsOver(toolNamed("listUsers", "List users", "Answers every stored user"));

    assertThat(directMcpTranslation.toolSpecifications())
        .extracting(specification -> specification.tool().title())
        .containsExactly("List users");
  }

  @Test
  void callAToolWithoutARequiredArgument_answersAnErrorResultInsteadOfThrowing() throws Exception {
    DirectMcpTranslation directMcpTranslation = directToolsOver(toolRequiringAPathId());

    McpSchema.CallToolResult result = callFirstTool(directMcpTranslation, Map.of());

    assertThat(result.isError()).isTrue();
    assertThat(textOf(result)).contains("id");
    assertThat(callExecutor.calls()).isEmpty();
  }

  @Test
  void callAToolWithItsArguments_passesTheBoundCallToTheExecutor() throws Exception {
    DirectMcpTranslation directMcpTranslation = directToolsOver(toolRequiringAPathId());

    McpSchema.CallToolResult result = callFirstTool(directMcpTranslation, Map.of("id", "u1"));

    assertThat(result.isError()).isFalse();
    assertThat(callExecutor.onlyCall().pathVariables()).containsExactly(Map.entry("id", "u1"));
  }

  @Test
  void toolSpecifications_rejectsTwoToolsThatSanitizeToTheSameName() {
    ToolCatalog catalog =
        ToolCatalog.of(
            List.of(toolNamed("get user", null, null), toolNamed("get_user", null, null)));
    ToolCatalogSource source = () -> catalog;
    DirectMcpTranslation directMcpTranslation =
        new DirectMcpTranslation(new Toolpool(source, callExecutor));

    assertThatThrownBy(directMcpTranslation::toolSpecifications)
        .isInstanceOf(DuplicatePublishedNameException.class)
        .hasMessageContaining("get user")
        .hasMessageContaining("get_user");
  }

  private DirectMcpTranslation directToolsFor(String specLocation) {
    return new DirectMcpTranslation(
        new Toolpool(ToolpoolFixtures.catalogSourceFor(specLocation), callExecutor));
  }

  private DirectMcpTranslation directToolsOver(Tool tool) {
    ToolCatalog catalog = ToolCatalog.of(List.of(tool));
    ToolCatalogSource source = () -> catalog;
    return new DirectMcpTranslation(new Toolpool(source, callExecutor));
  }

  private static McpSchema.CallToolResult callFirstTool(
      DirectMcpTranslation directMcpTranslation, Map<String, Object> arguments) throws Exception {
    SyncToolSpecification specification = directMcpTranslation.toolSpecifications().getFirst();
    return specification
        .callHandler()
        .apply(null, new McpSchema.CallToolRequest(specification.tool().name(), arguments, null));
  }

  private static Tool toolNamed(String rawName, String summary, String description) {
    return new Tool(
        new ToolName(rawName),
        new HttpTarget(HttpMethod.GET, "/users"),
        new ToolDocumentation(summary, description, List.of()),
        List.of());
  }

  private static Tool toolRequiringAPathId() {
    return new Tool(
        new ToolName("getUser"),
        new HttpTarget(HttpMethod.GET, "/users/{id}"),
        new ToolDocumentation("Get a user", null, List.of()),
        List.of(
            new ToolParameter(
                "id",
                ParameterLocation.PATH,
                true,
                new JsonSchema(Map.of("type", "string")),
                null)));
  }

  private static String textOf(McpSchema.CallToolResult result) {
    return ((McpSchema.TextContent) result.content().getFirst()).text();
  }
}
