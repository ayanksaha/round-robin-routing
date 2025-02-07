package com.codapayments.routing.config.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ServerConfigList {
    private List<ServerConfig> routes;
}
