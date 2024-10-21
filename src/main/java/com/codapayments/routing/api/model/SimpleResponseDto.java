package com.codapayments.routing.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class SimpleResponseDto {
    private String game;

    @JsonProperty("gamer_id")
    private String gamerId;

    private Integer points;
}
