package com.codapayments.routing.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SimpleRequestDto(
    String game,
    @JsonProperty("gamer_id") String gamerId,
    Integer points) {
}
