package com.standupiq.standup_iq.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI standupIqOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("StandupIQ API")
                        .version("0.1.0")
                        .description("AI-powered daily standup generator that summarizes GitHub activity with Gemini.")
                        .license(new License().name("Portfolio Project")));
    }
}
