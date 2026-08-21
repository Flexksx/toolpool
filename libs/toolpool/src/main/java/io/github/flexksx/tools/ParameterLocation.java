package io.github.flexksx.tools;

import java.util.Arrays;
import java.util.Optional;

public enum ParameterLocation {
  PATH,
  QUERY,
  HEADER,
  COOKIE;

  static Optional<ParameterLocation> of(String openApiLocation) {
    return Arrays.stream(values())
        .filter(location -> location.name().equalsIgnoreCase(openApiLocation))
        .findFirst();
  }
}
