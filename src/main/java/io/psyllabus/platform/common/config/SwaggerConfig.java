package io.psyllabus.platform.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String PLATFORM_KEY = "platformKey";
    private static final String TENANT_HEADER = "tenantHeader";

    @Bean
    public OpenAPI customOpenApi(){
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_AUTH)
                        .addList(PLATFORM_KEY)
                        .addList(TENANT_HEADER))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, jwtSecurityScheme())
                        .addSecuritySchemes(PLATFORM_KEY, platformKeySecurityScheme())
                        .addSecuritySchemes(TENANT_HEADER, tenantHeaderScheme()));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT access token. Obtain from /api/auth/login");
    }

    private SecurityScheme platformKeySecurityScheme() {
        return new SecurityScheme()
                .name("X-Platform-Key")
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .description("Platform-level API key required for provisioning endpoints");
    }

    private SecurityScheme tenantHeaderScheme() {
        return new SecurityScheme()
                .name("X-Tenant-ID")
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .description("School tenant identifier (subdomain or UUID). Required for all school-scoped endpoints. e.g. 'stmary' or 'riverside'");
    }

    private List<Server> serverList() {
        return List.of(
                new Server().url("http://localhost:8080").description("Development"),
                new Server().url("https://staging-api.psyllabus.io").description("Staging"),
                new Server().url("https://api.psyllabus.io").description("Production")
        );
    }

    private Info apiInfo() {
            return new Info()
                    .title(applicationName)
                    .description("Psyllabus Platform API — Multi-tenant School Management System with Stakeholder Intelligence.")
                    .version("1.0.0")
                    .contact(new Contact()
                            .name("Psyllabus Engineering")
                            .email("support@psyllabus.io")
                    );
    }
}
