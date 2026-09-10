package io.github.flexksx.toolpool.testing;

import io.github.flexksx.toolpool.adapter.openapi.OpenApiToolCatalogSource;
import io.github.flexksx.toolpool.application.ToolCallExecutor;
import io.github.flexksx.toolpool.application.ToolCatalogSource;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.tool.ToolCall;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import java.util.ArrayList;
import java.util.List;

public final class ToolpoolFixtures {

  public static final String SAMPLE_SPEC = "openapi-specs/sample-rest-api-client.openapi.json";

  private ToolpoolFixtures() {}

  public static ToolCatalogSource catalogSourceFor(String specLocation) {
    return new OpenApiToolCatalogSource(specLocation);
  }

  public static Toolpool toolpoolFor(String specLocation, ToolCallExecutor callExecutor) {
    return new Toolpool(catalogSourceFor(specLocation), callExecutor);
  }

  public static final class RecordingCallExecutor implements ToolCallExecutor {

    private final List<ToolCall> calls = new ArrayList<>();
    private final ToolCallResult answer;

    public RecordingCallExecutor(ToolCallResult answer) {
      this.answer = answer;
    }

    @Override
    public ToolCallResult execute(ToolCall call) {
      calls.add(call);
      return answer;
    }

    public List<ToolCall> calls() {
      return List.copyOf(calls);
    }

    public ToolCall onlyCall() {
      return calls.getFirst();
    }
  }
}
