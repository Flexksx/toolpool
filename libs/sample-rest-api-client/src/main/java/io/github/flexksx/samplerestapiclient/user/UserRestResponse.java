package io.github.flexksx.samplerestapiclient.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserRestResponse(@JsonProperty("id") String id, @JsonProperty("name") String name) {}
