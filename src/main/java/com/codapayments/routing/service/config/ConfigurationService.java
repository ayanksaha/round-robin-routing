package com.codapayments.routing.service.config;

import com.codapayments.routing.config.model.ServerConfig;
import com.codapayments.routing.config.model.ServerConfigList;
import com.codapayments.routing.service.heartbeat.HeartbeatService;
import com.codapayments.routing.service.model.ServerInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ConfigurationService {
    @Autowired
    private Map<UUID, ServerInstance> configuredServers;
    @Autowired
    private HeartbeatService heartbeatService;
    /**
     * Configures all the servers from `servers.yml` and if active, assigns them to either of active and stale server set
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initialisation started...");
            final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            final ServerConfigList config = objectMapper
                    .readValue(getClass().getResourceAsStream("/servers.yml"), ServerConfigList.class);
            for (ServerConfig serverConfig : config.getRoutes()) {
                final UUID instanceId = UUID.randomUUID();
                log.info("Configured server {} with instanceId: {}", serverConfig.getUri(), instanceId);
                configuredServers.put(instanceId,
                        ServerInstance.builder()
                                .instanceId(instanceId)
                                .uri(serverConfig.getUri())
                                .healthApiPath(serverConfig.getHealth())
                                .resourceApiPath(serverConfig.getPath())
                                .build());
            }
            formActiveServerList();
            log.info("Initialising successful.. Registered {} routes.", configuredServers.size());
        } catch (Exception e) {
            log.error("Initialisation Failed!");
        }
    }

    private void formActiveServerList() {
        for (final Map.Entry<UUID, ServerInstance> configuredInstance : configuredServers.entrySet()) {
            final ServerInstance instance = configuredInstance.getValue();
            heartbeatService.reAssesServerHealth(configuredInstance.getKey(), instance);
        }
    }
}
