package com.undoschool.bookingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bookingServiceOpenApi() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080/booking").description("API Gateway"),
                        new Server().url("http://localhost:8082").description("Booking Service")
                ))
                .info(new Info()
                        .title("Booking Service API")
                        .version("1.0.0")
                        .description("Parent-facing offering discovery and booking APIs."));
    }
}
