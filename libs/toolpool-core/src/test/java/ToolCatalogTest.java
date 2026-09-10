import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.http.HttpMethod;
import io.github.flexksx.toolpool.domain.http.HttpTarget;
import io.github.flexksx.toolpool.domain.tool.DuplicateToolNameException;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import io.github.flexksx.toolpool.domain.tool.ToolDocumentation;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ToolCatalogTest {

  private static final ToolName LIST_USERS = new ToolName("listUsers");

  private final Tool listUsers = toolNamed(LIST_USERS, "/users", "List every user");
  private final Tool createUser = toolNamed(new ToolName("createUser"), "/users", "Add a user");

  @Test
  void findAToolByItsName_returnsThatTool() throws Exception {
    ToolCatalog catalog = ToolCatalog.of(List.of(listUsers, createUser));

    assertThat(catalog.find(LIST_USERS)).isSameAs(listUsers);
    assertThat(catalog.tools()).containsExactly(listUsers, createUser);
  }

  @Test
  void findAnUnknownToolName_throwsNamingTheTool() {
    ToolCatalog catalog = ToolCatalog.of(List.of(listUsers));

    assertThatThrownBy(() -> catalog.find(new ToolName("deleteUser")))
        .isInstanceOf(UnknownToolException.class)
        .hasMessageContaining("deleteUser");
  }

  @Test
  void ofTwoToolsThatShareAName_throwsNamingBothTargets() {
    Tool clashing = toolNamed(LIST_USERS, "/people", "List every person");

    assertThatThrownBy(() -> ToolCatalog.of(List.of(listUsers, clashing)))
        .isInstanceOf(DuplicateToolNameException.class)
        .hasMessageContainingAll("listUsers", "/users", "/people");
  }

  @Test
  void searchWithAQuery_keepsOnlyTheMatchingTools() {
    ToolCatalog catalog = ToolCatalog.of(List.of(listUsers, createUser));

    assertThat(catalog.search("add")).containsExactly(createUser);
    assertThat(catalog.search("")).containsExactly(listUsers, createUser);
  }

  private static Tool toolNamed(ToolName name, String path, String summary) {
    return new Tool(
        name,
        new HttpTarget(HttpMethod.GET, path),
        new ToolDocumentation(summary, null, List.of()),
        List.of());
  }
}
