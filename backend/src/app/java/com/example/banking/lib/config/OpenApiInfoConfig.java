package com.example.banking.lib.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiInfoConfig {

    @Bean
    OpenAPI bankingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NorthBridge Banking API")
                        .version("v1")
                        .description("Interactive API documentation for the NorthBridge banking backend. "
                                + "Each operation includes a summary and a clear description of business purpose."));
    }
}
