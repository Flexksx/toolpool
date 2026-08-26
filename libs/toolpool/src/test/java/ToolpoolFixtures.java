import io.github.flexksx.adapter.openapi.OpenApiToolCatalogProvider;
import io.github.flexksx.application.ToolCallExecutor;
import io.github.flexksx.application.ToolCatalogProvider;
import io.github.flexksx.application.Toolpool;
import io.github.flexksx.domain.tool.ToolCall;
import io.github.flexksx.domain.tool.ToolCallResult;
import java.util.ArrayList;
import java.util.List;

final class ToolpoolFixtures {

  static final String SAMPLE_SPEC = "openapi-specs/sample-rest-api-client.openapi.json";

  private ToolpoolFixtures() {}

  static ToolCatalogProvider catalogProviderFor(String specLocation) {
    return new OpenApiToolCatalogProvider(specLocation);
  }

  static Toolpool toolpoolFor(String specLocation, ToolCallExecutor callExecutor) {
    return new Toolpool(catalogProviderFor(specLocation), callExecutor);
  }

  static final class RecordingCallExecutor implements ToolCallExecutor {

    private final List<ToolCall> calls = new ArrayList<>();
    private final ToolCallResult answer;

    RecordingCallExecutor(ToolCallResult answer) {
      this.answer = answer;
    }

    @Override
    public ToolCallResult execute(ToolCall call) {
      calls.add(call);
      return answer;
    }

    List<ToolCall> calls() {
      return List.copyOf(calls);
    }

    ToolCall onlyCall() {
      return calls.getFirst();
    }
  }
}
