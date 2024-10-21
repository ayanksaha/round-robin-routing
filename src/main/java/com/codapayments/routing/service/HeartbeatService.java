package com.codapayments.routing.service;

import static com.codapayments.routing.common.ApplicationConstants.ACTIVE_SERVERS;
import static com.codapayments.routing.common.ApplicationConstants.OK;
import static com.codapayments.routing.common.ApplicationConstants.STALE_SERVERS;
import static com.codapayments.routing.common.ApplicationConstants.SERVER_QUEUE;

import com.codapayments.routing.config.model.ServerConfig;
import com.codapayments.routing.config.model.ServerConfigList;
import com.codapayments.routing.service.model.ServerInstance;
import com.codapayments.routing.service.model.StatusResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Queue;
import java.util.HashMap;
import java.util.Objects;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;


/***
 * The following are used to keep track of all the defined servers
 * configuredServers: All servers configured in servers.yml are stored here
 * activeServers: All currently active servers are placed here
 * staleServers: All servers that have gone stale are placed here
 * serverQueue: This is the data structure that helps us achieve the Round Robin algorithm
 */
@Service
@Slf4j
public class HeartbeatService {

    private final Map<UUID, ServerInstance> configuredServers;
    private final Set<UUID> activeServers;
    private final Set<UUID> staleServers;
    private final Queue<UUID> serverQueue;
    @Autowired
    private RestTemplate restTemplate;

    public HeartbeatService() {
        configuredServers = new HashMap<>();
        activeServers = new HashSet<>();
        staleServers = new HashSet<>();
        serverQueue = new LinkedList<>();
    }

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

    /***
     *
     * @return the next available server in a Round Robin fashion
     * serverQueue helps achieve this
     */
    @Synchronized
    public ServerInstance findNextActiveServer() {
        log.debug("Server Queue: {}", serverQueue);
        final UUID nextActiveServerId = serverQueue.poll();
        log.info("Next active server: {}", nextActiveServerId);
        serverQueue.add(nextActiveServerId);
        return configuredServers.get(nextActiveServerId);
    }

    /***
     * This heartbeat process runs every second to check for the health of all the active servers
     * and removes all the stale servers from the active server set
     */
    @Scheduled(fixedRate = 1000)
    @Synchronized
    public void refreshActiveServerList() {
        for (final UUID instanceId : activeServers) {
            final ServerInstance instance = configuredServers.get(instanceId);
            reAssesServerHealth(instanceId, instance);
        }
    }

    /***
     * This heartbeat process runs every 5 second to check for the health of all the stale servers
     * and adds back all the healthy servers to the active server set
     */
    @Scheduled(fixedRate = 5000)
    @Synchronized
    public void refreshStaleServerList() {
        for (final UUID instanceId : staleServers) {
            final ServerInstance instance = configuredServers.get(instanceId);
            reAssesServerHealth(instanceId, instance);
        }
    }

    /***
     *
     * @return a map that contains:
     * server_queue: The active servers in the actual order they will serve
     * active_servers: The entire set of active servers
     * stale_servers: The entire set of stale servers
     */
    public Map<String, List<ServerInstance>> getStatus() {
        Map<String, List<ServerInstance>> statusMap = new LinkedHashMap<>();
        statusMap.put(SERVER_QUEUE, getServerQueue());
        statusMap.put(ACTIVE_SERVERS, getActiveServers());
        statusMap.put(STALE_SERVERS, getStaleServers());
        return statusMap;
    }

    public void removeActiveServer(final UUID instanceId) {
        activeServers.remove(instanceId);
        serverQueue.remove(instanceId);
        staleServers.add(instanceId);
    }

    private void addActiveServer(final UUID instanceId) {
        activeServers.add(instanceId);
        serverQueue.add(instanceId);
        staleServers.remove(instanceId);
    }

    private void formActiveServerList() {
        for (final Map.Entry<UUID, ServerInstance> configuredInstance : configuredServers.entrySet()) {
            final ServerInstance instance = configuredInstance.getValue();
            reAssesServerHealth(configuredInstance.getKey(), instance);
        }
    }

    private void reAssesServerHealth(final UUID instanceId, final ServerInstance instance) {
        try {
            final StatusResponseDto status = restTemplate.getForObject(
                    instance.getUri() + instance.getHealthApiPath(), StatusResponseDto.class);
            if (Objects.nonNull(status) && OK.equals(status.getStatus())
                    && !activeServers.contains(instanceId)) {
                addActiveServer(instanceId);
                log.info("Added server instance {} to active server list", instance.getInstanceId());
            }
        } catch (ResourceAccessException e) {
            log.error("ResourceAccessException occurred while calling health API for server instance: {}",
                    instance.getInstanceId());
            if (!staleServers.contains(instanceId)) {
                removeActiveServer(instanceId);
                log.info("Removed server instance {} from active server list", instance.getInstanceId());
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            log.error("HttpClientErrorException with status code {} occurred while calling health API for server instance: {}",
                    statusCode, instance.getInstanceId());
        } catch (Exception e) {
            log.error("Exception {} occurred while calling health API for server instance: {}", e.getMessage(),
                    instance.getInstanceId());
        }
    }

    private List<ServerInstance> getServerQueue() {
        final List<ServerInstance> servers = new ArrayList<>();
        getServerConfigs(servers, serverQueue);
        return servers;
    }

    private List<ServerInstance> getActiveServers() {
        final List<ServerInstance> servers = new ArrayList<>();
        getServerConfigs(servers, activeServers);
        return servers;
    }

    private List<ServerInstance> getStaleServers() {
        final List<ServerInstance> servers = new ArrayList<>();
        getServerConfigs(servers, staleServers);
        return servers;
    }

    private <T extends Iterable<UUID>> void getServerConfigs(final List<ServerInstance> result, final T serverIds) {
        for (final UUID serverId : serverIds) {
            result.add(configuredServers.get(serverId));
        }
    }
}
