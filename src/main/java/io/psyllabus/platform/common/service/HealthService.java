package io.psyllabus.platform.common.service;

import io.psyllabus.platform.common.dto.HealthResponse;
import io.psyllabus.platform.common.dto.HealthResponse.ServiceHealth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Pings all infrastructure containers to determine overall system health.
 * Used by {@code GET /api/health} — public endpoint, no auth required.
 *
 * <p>TCP socket checks for Postgres/Redis/RabbitMQ (mirrors Docker health checks).
 * HTTP checks for Prometheus/Grafana/Loki/Alloy.
 */
@Service
@Slf4j
public class HealthService {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 3000;

    @Value("${spring.datasource.host:localhost}")
    private String dbHost;

    @Value("${spring.datasource.port:5432}")
    private int dbPort;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitPort;

    @Value("${spring.boot.admin.client.url:http://localhost:9090}")
    private String prometheusUrl;

    @Value("${grafana.url:http://localhost:3001}")
    private String grafanaUrl;

    @Value("${loki.url:http://localhost:3100}")
    private String lokiUrl;

    @Value("${alloy.url:http://localhost:12345}")
    private String alloyUrl;

    /**
     * Check all containers and return aggregated health response.
     */
    public HealthResponse checkAll() {
        List<ServiceHealth> services = new ArrayList<>();

        services.add(checkDatabase());
        services.add(checkCache());
        services.add(checkQueue());
        services.add(checkMetrics());
        services.add(checkMonitoring());
        services.add(checkLogging());
        services.add(checkAgent());

        return HealthResponse.from(services);
    }

    public ServiceHealth checkDatabase() {
        return tcpCheck("Postgres", dbHost, dbPort, "pg_isready");
    }

    public ServiceHealth checkCache() {
        return tcpCheck("Redis", redisHost, redisPort, "redis-cli ping");
    }

    public ServiceHealth checkQueue() {
        return tcpCheck("RabbitMQ", rabbitHost, rabbitPort, "rabbitmq-diagnostics ping");
    }

    public ServiceHealth checkMetrics() {
        return httpCheck("Prometheus", prometheusUrl + "/-/healthy");
    }

    public ServiceHealth checkMonitoring() {
        return httpCheck("Grafana", grafanaUrl + "/api/health");
    }

    public ServiceHealth checkLogging() {
        return httpCheck("Loki", lokiUrl + "/ready");
    }

    public ServiceHealth checkAgent() {
        return httpCheck("Alloy", alloyUrl + "/");
    }

    private ServiceHealth tcpCheck(String name, String host, int port, String hint) {
        long start = System.currentTimeMillis();
        try (var socket = new java.net.Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            log.debug("{} health check OK ({}ms)", name, latency);
            return new ServiceHealth(name, "UP", latency, null);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("{} health check FAILED: {}", name, e.getMessage());
            return new ServiceHealth(name, "DOWN", latency, e.getMessage());
        }
    }

    private ServiceHealth httpCheck(String name, String url) {
        long start = System.currentTimeMillis();
        try {
            URI uri = URI.create(url);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();

            long latency = System.currentTimeMillis() - start;
            if (code >= 200 && code < 400) {
                log.debug("{} health check OK (HTTP {} in {}ms)", name, code, latency);
                return new ServiceHealth(name, "UP", latency, null);
            } else {
                log.warn("{} health check returned HTTP {}", name, code);
                return new ServiceHealth(name, "DOWN", latency, "HTTP " + code);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("{} health check FAILED: {}", name, e.getMessage());
            return new ServiceHealth(name, "DOWN", latency, e.getMessage());
        }
    }
}