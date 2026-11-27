package com.abc.sample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * API-level metadata for the generated OpenAPI document ({@code /v3/api-docs}) and
 * Swagger UI ({@code /swagger-ui.html}).
 *
 * <p>Declares the {@value #BEARER_AUTH} security scheme. Protected controllers reference it
 * with {@code @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)}, which lets Swagger UI
 * send the token entered under "Authorize".
 */
@Configuration
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI sampleApiOpenApi() {
		return new OpenAPI()
			.info(new Info().title("Sample API")
				.version("v1")
				.description("Spring Boot sample REST API: Luhn credit-card validation and Actuator health. "
						+ "Get a token from POST /api/auth/token, then send it as `Authorization: Bearer <token>`."))
			.components(new Components().addSecuritySchemes(BEARER_AUTH,
					new SecurityScheme().type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description("Access token from POST /api/auth/token")));
	}

}
