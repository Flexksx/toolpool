import io.github.flexksx.toolpool.domain.http.HttpMethod;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolDocumentation;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.ToolParameter;
import java.util.List;

final class ToolExamples {

  static final ToolName NAME_GET_USER = new ToolName("getUser");
  static final HttpTarget TARGET_GET_USER = new HttpTarget(HttpMethod.GET, "/users/{id}");

  private ToolExamples() {}

  static Tool toolWith(List<ToolParameter> parameters) {
    return new Tool(
        NAME_GET_USER, TARGET_GET_USER, new ToolDocumentation(null, null, List.of()), parameters);
  }

  static Tool toolDocumentedAs(ToolDocumentation documentation) {
    return new Tool(NAME_GET_USER, TARGET_GET_USER, documentation, List.of());
  }
}
