package io.psyllabus.platform.common.controller;

import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.HealthResponse;
import io.psyllabus.platform.common.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public health check endpoint — no authentication required.
 * Ping all infrastructure containers and return overall status.
 *
 * <p>Returns HTTP 200 if all services are UP, HTTP 503 if any service is DOWN.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health", description = "Public infrastructure health checks")
public class HealthController {

    private final HealthService healthService;

    /**
     * Check all containers and return their status.
     *
     * <p>Containers checked:
     * <ul>
     *   <li>Postgres (TCP 5432)</li>
     *   <li>Redis (TCP 6379)</li>
     *   <li>RabbitMQ (TCP 5672)</li>
     *   <li>Prometheus (HTTP /-/healthy)</li>
     *   <li>Grafana (HTTP /api/health)</li>
     *   <li>Loki (HTTP /ready)</li>
     *   <li>Alloy (HTTP /)</li>
     * </ul>
     *
     * @return 200 with health status, or 503 if any service is DOWN
     */
    @GetMapping
    @Operation(summary = "Check all infrastructure containers")
    public ResponseEntity<ApiResponse<HealthResponse>> check() {
        log.info("Health check requested");
        HealthResponse health = healthService.checkAll();

        if ("PASS".equals(health.overallStatus())) {
            return ResponseEntity.ok(ApiResponse.success(health, "All services healthy"));
        } else {
            return ResponseEntity.status(503)
                    .body(ApiResponse.success(health, "One or more services degraded"));
        }
    }
}