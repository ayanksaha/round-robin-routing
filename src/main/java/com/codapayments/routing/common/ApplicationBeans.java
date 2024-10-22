package com.codapayments.routing.common;

import com.codapayments.routing.service.model.ServerInstance;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Component
public class ApplicationBeans {
    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(300))
                .build();
    }
    @Bean(name = "configuredServers")
    public Map<UUID, ServerInstance> getConfiguredServers() {
        return new HashMap<>();
    }
    @Bean(name = "activeServers")
    public Set<UUID> getActiveServers() {
        return new HashSet<>();
    }
    @Bean(name = "serverQueue")
    public Queue<UUID> getServerQueue() {
        return new LinkedList<>();
    }
    @Bean(name = "staleServers")
    public Set<UUID> getStaleServers() {
        return new HashSet<>();
    }
}
