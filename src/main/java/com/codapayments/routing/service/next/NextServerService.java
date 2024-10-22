package com.codapayments.routing.service.next;

import com.codapayments.routing.service.model.ServerInstance;

public interface NextServerService {
    ServerInstance findNextActiveServer();
}
