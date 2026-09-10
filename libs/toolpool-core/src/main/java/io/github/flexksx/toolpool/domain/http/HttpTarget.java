package io.github.flexksx.toolpool.domain.http;

public record HttpTarget(HttpMethod method, String path) {

  public HttpTarget {
    if (path.isBlank()) {
      throw new IllegalArgumentException("A target path cannot be blank");
    }
  }

  public String describe() {
    return method.name() + " " + path;
  }
}
