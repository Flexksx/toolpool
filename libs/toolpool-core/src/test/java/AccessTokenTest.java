import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.flexksx.toolpool.domain.auth.AccessToken;
import org.junit.jupiter.api.Test;

public class AccessTokenTest {

  @Test
  void toString_hidesTheTokenValue() {
    assertThat(new AccessToken("secret-value").toString()).doesNotContain("secret-value");
  }

  @Test
  void constructWithABlankValue_throws() {
    assertThatThrownBy(() -> new AccessToken(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be blank");
  }
}
