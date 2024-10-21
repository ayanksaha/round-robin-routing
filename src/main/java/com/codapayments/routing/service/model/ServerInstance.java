package com.codapayments.routing.service.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ServerInstance {
    private UUID instanceId;
    private String uri;
    private String healthApiPath;
    private String resourceApiPath;
}
