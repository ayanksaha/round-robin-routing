package com.codapayments.routing.service.next;

import com.codapayments.routing.service.model.ServerInstance;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

@Service
@Slf4j
public class RoundRobinService implements NextServerService {
    @Autowired
    private Queue<UUID> serverQueue;
    @Autowired
    private Map<UUID, ServerInstance> configuredServers;

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
        if(Objects.nonNull(nextActiveServerId)) {
            serverQueue.add(nextActiveServerId);
        }
        return configuredServers.get(nextActiveServerId);
    }
}
