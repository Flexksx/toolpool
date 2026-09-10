package io.github.flexksx.toolpool.testing;

import io.github.flexksx.toolpool.adapter.openapi.OpenApiToolCatalogSource;
import io.github.flexksx.toolpool.application.ToolCallExecutor;
import io.github.flexksx.toolpool.application.ToolCatalogSource;
import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
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

    private final List<ToolCallRequest> calls = new ArrayList<>();
    private final ToolCallResult answer;

    public RecordingCallExecutor(ToolCallResult answer) {
      this.answer = answer;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest call) {
      calls.add(call);
      return answer;
    }

    public List<ToolCallRequest> calls() {
      return List.copyOf(calls);
    }

    public ToolCallRequest onlyCall() {
      return calls.getFirst();
    }
  }
}
