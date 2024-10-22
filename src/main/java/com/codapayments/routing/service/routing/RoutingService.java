package com.codapayments.routing.service.routing;

import com.codapayments.routing.api.model.SimpleRequestDto;
import com.codapayments.routing.api.model.SimpleResponseDto;
import com.codapayments.routing.persistence.model.RoutingLog;
import com.codapayments.routing.persistence.repositories.LogRepository;
import com.codapayments.routing.service.heartbeat.HeartbeatService;
import com.codapayments.routing.service.model.ServerInstance;
import com.codapayments.routing.service.next.NextServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class RoutingService {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LogRepository logRepository;
    @Autowired
    private HeartbeatService heartbeatService;
    @Autowired
    private NextServerService nextServerService;

    /***
     *
     * @param request: Request to the downstream API
     * @return response from the downstream API
     * It checks the next active server from the heartbeat service and calls the downstream API
     */
    public SimpleResponseDto postRequest(final SimpleRequestDto request) {
        final ServerInstance instance = nextServerService.findNextActiveServer();
        if (Objects.isNull(instance)) throw new RuntimeException("No active server..");
        try {
            final LocalDateTime requestSentAt = LocalDateTime.now();
            log.info("Request routed to server instance {}, uri: {}", instance.getInstanceId(), instance.getUri());
            final SimpleResponseDto response = restTemplate.postForObject(
                    instance.getUri() + instance.getResourceApiPath(), request, SimpleResponseDto.class);
            log.info("Got successful response from server instance {}", instance.getInstanceId());
            final LocalDateTime responseReceivedAt = LocalDateTime.now();
            logRequestToDb(instance, requestSentAt, responseReceivedAt);
            return response;
        } catch (ResourceAccessException e) {
            log.error("ResourceAccessException occurred while calling resource API for server instance: {}",
                    instance.getInstanceId());
            heartbeatService.removeActiveServer(instance.getInstanceId());
            throw e;
        } catch (HttpClientErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            log.error("HttpClientErrorException with status code {} occurred while calling health API for server instance: {}",
                    statusCode, instance.getInstanceId());
            throw e;
        } catch (Exception e) {
            log.error("Exception {} occurred while calling resource API for server instance: {}", e.getMessage(),
                    instance.getInstanceId());
            throw e;
        }
    }

    @Async
    private void logRequestToDb(ServerInstance instance, LocalDateTime requestSentAt, LocalDateTime responseReceivedAt) {
        final RoutingLog logItem = new RoutingLog();
        logItem.setUri(instance.getUri());
        logItem.setServerId(instance.getInstanceId());
        logItem.setRequestedAt(requestSentAt);
        logItem.setResponseTime(Duration.between(requestSentAt, responseReceivedAt).toMillis());
        logRepository.save(logItem);
        log.debug("Added Log {} to database", logItem);
    }

}
