package io.psyllabus.platform.common.config;

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

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenApi(){
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .schemaRequirement(SECURITY_SCHEME_NAME, securityScheme());
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT from /api/auth/login");
    }

    private List<Server> serverList() {
        return List.of(
                new Server().url("http://localhost:8080").description("local"),
                new Server().url("https://api.psyllabus.io").description("Production")
        );
    }

    private Info apiInfo() {
            return new Info()
                    .title(applicationName)
                    .description("Multi-tenant school management system")
                    .version("0.0.1")
                    .contact(new Contact()
                            .name("Psyllabus Platform")
                            .email("suppoort@psyllabus.io")
                    );
    }
}
