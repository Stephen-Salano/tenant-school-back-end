package io.psyllabus.platform.common.dto;

import java.util.List;

/**
 * Aggregated health check response for all infrastructure containers.
 * Returned by {@code GET /api/health} (public, no auth).
 *
 * @param database   Postgres status
 * @param cache      Redis status
 * @param queue      RabbitMQ status
 * @param metrics    Prometheus status
 * @param monitoring Grafana status
 * @param logging    Loki status
 * @param agent      Alloy status
 * @param overallStatus PASS if all UP, FAIL otherwise
 */
public record HealthResponse(
        ServiceHealth database,
        ServiceHealth cache,
        ServiceHealth queue,
        ServiceHealth metrics,
        ServiceHealth monitoring,
        ServiceHealth logging,
        ServiceHealth agent,
        String overallStatus
) {
    public record ServiceHealth(
            String name,
            String status,
            long latencyMs,
            String error
    ) {}

    public static HealthResponse allUp(
            ServiceHealth database,
            ServiceHealth cache,
            ServiceHealth queue,
            ServiceHealth metrics,
            ServiceHealth monitoring,
            ServiceHealth logging,
            ServiceHealth agent
    ) {
        return new HealthResponse(
                database, cache, queue, metrics, monitoring, logging, agent,
                "PASS"
        );
    }

    public static HealthResponse from(List<ServiceHealth> services) {
        boolean allUp = services.stream().allMatch(s -> "UP".equals(s.status()));
        String overall = allUp ? "PASS" : "DEGRADED";
        ServiceHealth db = services.get(0);
        ServiceHealth cache = services.get(1);
        ServiceHealth queue = services.get(2);
        ServiceHealth metrics = services.get(3);
        ServiceHealth monitoring = services.get(4);
        ServiceHealth logging = services.get(5);
        ServiceHealth agent = services.get(6);
        return new HealthResponse(db, cache, queue, metrics, monitoring, logging, agent, overall);
    }
}