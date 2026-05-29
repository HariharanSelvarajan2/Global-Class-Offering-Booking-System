package com.undoschool.courseservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI courseServiceOpenApi() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080/course").description("API Gateway"),
                        new Server().url("http://localhost:8081").description("Course Service")
                ))
                .info(new Info()
                        .title("Course Service API")
                        .version("1.0.0")
                        .description("Teacher-facing course, offering, and session APIs."));
    }
}
