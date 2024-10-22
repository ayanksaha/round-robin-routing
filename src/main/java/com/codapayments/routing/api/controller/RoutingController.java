package com.codapayments.routing.api.controller;

import com.codapayments.routing.service.heartbeat.HeartbeatService;
import com.codapayments.routing.service.routing.RoutingService;
import com.codapayments.routing.service.model.ServerInstance;
import com.codapayments.routing.api.model.SimpleRequestDto;
import com.codapayments.routing.api.model.SimpleResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class RoutingController {
    @Autowired
    private RoutingService routingService;
    @Autowired
    private HeartbeatService heartbeatService;

    @PostMapping("/route/simple/api")
    public ResponseEntity<SimpleResponseDto> postRequest(@RequestBody SimpleRequestDto request) {
        log.info("Request received for simple api");
        SimpleResponseDto response = routingService.postRequest(request);
        log.info("Response sent back");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/route/servers/status")
    public ResponseEntity<Map<String, List<ServerInstance>>> getStatus() {
        log.info("Request received for active servers");
        return ResponseEntity.ok(heartbeatService.getStatus());
    }
}
