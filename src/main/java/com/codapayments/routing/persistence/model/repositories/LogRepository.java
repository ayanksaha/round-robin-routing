package com.codapayments.routing.persistence.model.repositories;

import com.codapayments.routing.persistence.model.RoutingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<RoutingLog, Long> {
}
