import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.application.Toolpool;
import io.github.flexksx.toolpool.application.UpstreamAccessTokenResolver;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.github.flexksx.toolpool.domain.tool.ToolCallRequest;
import io.github.flexksx.toolpool.domain.tool.ToolCallResult;
import io.github.flexksx.toolpool.domain.tool.ToolCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ToolpoolCallTest {

  private static final AccessToken CALLER_TOKEN = new AccessToken("caller-token");
  private static final ToolCallResult ANSWER = new ToolCallResult("{}", false);

  private final List<ToolCallRequest> sentCalls = new ArrayList<>();

  @Test
  void callWithTheDefaultResolver_sendsNoAccessToken() throws Exception {
    new Toolpool(ToolpoolCallTest::catalog, this::record)
        .call(ToolExamples.NAME_GET_USER, Map.of(), CALLER_TOKEN);

    assertThat(sentCalls.getFirst().accessToken()).isNull();
  }

  @Test
  void callWithThePassthroughResolver_sendsTheCallerToken() throws Exception {
    new Toolpool(ToolpoolCallTest::catalog, this::record, UpstreamAccessTokenResolver.passthrough())
        .call(ToolExamples.NAME_GET_USER, Map.of(), CALLER_TOKEN);

    assertThat(sentCalls.getFirst().accessToken()).isEqualTo(CALLER_TOKEN);
  }

  @Test
  void callWithACustomResolver_sendsTheResolvedToken() throws Exception {
    AccessToken exchanged = new AccessToken("upstream-token");

    new Toolpool(ToolpoolCallTest::catalog, this::record, callerToken -> exchanged)
        .call(ToolExamples.NAME_GET_USER, Map.of(), CALLER_TOKEN);

    assertThat(sentCalls.getFirst().accessToken()).isEqualTo(exchanged);
  }

  private static ToolCatalog catalog() {
    return ToolCatalog.of(List.of(ToolExamples.toolWith(List.of())));
  }

  private ToolCallResult record(ToolCallRequest call) {
    sentCalls.add(call);
    return ANSWER;
  }
}
