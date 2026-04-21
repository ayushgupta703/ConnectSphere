package com.connectsphere.postservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI postServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere Post Service API")
                        .description("Handles post creation, updates, deletion, and retrieval")
                        .version("v1.0"));
    }
}