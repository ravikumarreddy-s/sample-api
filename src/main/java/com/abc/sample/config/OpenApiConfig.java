package com.abc.sample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * API-level metadata for the generated OpenAPI document ({@code /v3/api-docs}) and
 * Swagger UI ({@code /swagger-ui.html}).
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI sampleApiOpenApi() {
		return new OpenAPI().info(new Info().title("Sample API")
			.version("v1")
			.description("Spring Boot sample REST API: Luhn credit-card validation and Actuator health."));
	}

}
