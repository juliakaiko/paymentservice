package com.mymicroservice.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8084")
                                .description("Local server")
                ))
                .info(new Info()
                        .title("Payment Service API")
                        .description("Payment Service for microservices architecture")
                        .version("1.0.0")
                );
    }
}
