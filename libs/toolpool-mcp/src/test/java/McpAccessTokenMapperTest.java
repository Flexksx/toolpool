import static org.assertj.core.api.Assertions.assertThat;

import io.github.flexksx.toolpool.adapter.mcp.McpAccessTokenMapper;
import io.github.flexksx.toolpool.domain.auth.AccessToken;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class McpAccessTokenMapperTest {

  @Test
  void toCallerTokenOfABearerHeader_answersTheToken() {
    assertThat(callerTokenOf("Bearer t1")).isEqualTo(new AccessToken("t1"));
  }

  @Test
  void toCallerTokenOfALowerCaseBearerScheme_answersTheToken() {
    assertThat(callerTokenOf("bearer t1")).isEqualTo(new AccessToken("t1"));
  }

  @Test
  void toCallerTokenOfAnotherScheme_answersNoToken() {
    assertThat(callerTokenOf("Basic dXNlcjpwYXNz")).isNull();
  }

  @Test
  void toCallerTokenOfAnEmptyBearerHeader_answersNoToken() {
    assertThat(callerTokenOf("Bearer ")).isNull();
  }

  @Test
  void toCallerTokenOfNoHeader_answersNoToken() {
    assertThat(callerTokenOf(null)).isNull();
  }

  @Test
  void toCallerTokenOfNoExchange_answersNoToken() {
    assertThat(McpAccessTokenMapper.toCallerToken(null)).isNull();
  }

  private static @Nullable AccessToken callerTokenOf(@Nullable String authorizationHeader) {
    return McpAccessTokenMapper.toCallerToken(
        new McpSyncServerExchange(
            new McpAsyncServerExchange(
                "session",
                null,
                null,
                null,
                McpAccessTokenMapper.toTransportContext(authorizationHeader))));
  }
}
