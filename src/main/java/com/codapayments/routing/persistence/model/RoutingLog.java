package com.codapayments.routing.persistence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "log")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class RoutingLog {
    @Id
    @GeneratedValue
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID serverId;

    private String uri;

    private LocalDateTime requestedAt;

    private Long responseTime;
}
