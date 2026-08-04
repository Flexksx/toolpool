package io.github.flexksx.samplerestapiclient.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateUserRestRequest(@JsonProperty("name") String name) {}
