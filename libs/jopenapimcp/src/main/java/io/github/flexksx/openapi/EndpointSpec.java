package io.github.flexksx.openapi;

import java.util.Optional;

public record EndpointSpec(
    String operationId, Optional<String> summary, Optional<String> description) {}
