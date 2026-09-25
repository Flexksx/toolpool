package io.github.flexksx.toolpool.domain.auth;

public record AccessToken(String value) {

  private static final String REDACTED = "AccessToken[value=<redacted>]";

  public AccessToken {
    if (value.isBlank()) {
      throw new IllegalArgumentException("An access token cannot be blank");
    }
  }

  @Override
  public String toString() {
    return REDACTED;
  }
}
