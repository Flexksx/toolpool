package io.github.flexksx.toolpool.spring;

public enum ToolpoolAuthMode {
  NONE,

  PASSTHROUGH,

  TOKEN_EXCHANGE;

  public boolean requiresCallerToken() {
    return this != NONE;
  }
}
