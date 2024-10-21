package com.codapayments.routing.config.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServerConfig {
    private String uri;
    private String health;
    private String path;
}
