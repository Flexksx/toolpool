import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.application.port.out.ResolveUpstreamTokenPort;
import io.github.flexksx.toolpool.application.service.CallToolService;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.domain.tool.Tool;
import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import io.github.flexksx.toolpool.domain.tool.ToolName;
import io.github.flexksx.toolpool.domain.tool.UnknownToolException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class CallToolServiceTest {

  private static final AccessToken CALLER_TOKEN = new AccessToken("caller-token");
  private static final ToolCallResult ANSWER = new ToolCallResult("{}", false);

  private final List<ToolCallRequest> sentCalls = new ArrayList<>();

  @Test
  void callToolWithTheDefaultResolver_sendsNoAccessToken() throws Exception {
    new CallToolService(CallToolServiceTest::findTool, this::record)
        .callTool(ToolExamples.NAME_GET_USER, Map.of(), CALLER_TOKEN);

    assertThat(sentCalls.getFirst().accessToken()).isNull();
  }

  @Test
  void callToolWithThePassthroughResolver_sendsTheCallerToken() throws Exception {
    new CallToolService(
            CallToolServiceTest::findTool, this::record, ResolveUpstreamTokenPort.passthrough())
        .callTool(ToolExamples.NAME_GET_USER, Map.of(), CALLER_TOKEN);

    assertThat(sentCalls.getFirst().accessToken()).isEqualTo(CALLER_TOKEN);
  }

  @Test
  void callToolWithACustomResolver_sendsTheResolvedToken() throws Exception {
    AccessToken exchanged = new AccessToken("upstream-token");

    new CallToolService(CallToolServiceTest::findTool, this::record, callerToken -> exchanged)
        .callTool(ToolExamples.NAME_GET_USER, Map.of(), CALLER_TOKEN);

    assertThat(sentCalls.getFirst().accessToken()).isEqualTo(exchanged);
  }

  private static Tool findTool(ToolName name) throws UnknownToolException {
    return ToolCatalog.of(List.of(ToolExamples.toolWith(List.of()))).find(name);
  }

  private ToolCallResult record(ToolCallRequest call) {
    sentCalls.add(call);
    return ANSWER;
  }
}
