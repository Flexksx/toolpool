package io.github.flexksx.toolpool.application.port.out;

public class UpstreamAccessTokenUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UpstreamAccessTokenUnavailableException(String message) {
    super(message);
  }

  public UpstreamAccessTokenUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
