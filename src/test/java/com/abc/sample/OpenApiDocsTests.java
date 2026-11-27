package com.abc.sample;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The docs are fetched without a token throughout: they must be reachable anonymously. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTests {

	private static final String CARD_OP = "$.paths['/api/cards/validate'].post";

	private static final String TOKEN_OP = "$.paths['/api/auth/token'].post";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocsDescribeAllEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.info.title").value("Sample API"))
			.andExpect(jsonPath(CARD_OP + ".summary").value("Validate a card number"))
			.andExpect(jsonPath(CARD_OP + ".responses['200']").exists())
			.andExpect(jsonPath(CARD_OP + ".responses['400']").exists())
			.andExpect(jsonPath(CARD_OP + ".responses['401']").exists())
			.andExpect(jsonPath(TOKEN_OP + ".summary").value("Get an access token"))
			.andExpect(jsonPath(TOKEN_OP + ".responses['200']").exists())
			.andExpect(jsonPath(TOKEN_OP + ".responses['400']").exists())
			.andExpect(jsonPath(TOKEN_OP + ".responses['401']").exists())
			.andExpect(jsonPath(CARD_OP + ".responses['400'].content['application/problem+json'].schema['$ref']")
				.value("#/components/schemas/ProblemDetail"))
			.andExpect(jsonPath(TOKEN_OP + ".responses['401'].content['application/problem+json'].schema['$ref']")
				.value("#/components/schemas/ProblemDetail"))
			.andExpect(jsonPath("$.components.schemas.CardValidationRequest.required", hasItem("cardNumber")))
			.andExpect(jsonPath("$.components.schemas.CardValidationResponse.properties.valid").exists())
			.andExpect(jsonPath("$.components.schemas.TokenRequest.required", hasItem("password")))
			.andExpect(jsonPath("$.components.schemas.TokenRequest.properties.password.format").value("password"))
			.andExpect(jsonPath("$.components.schemas.TokenResponse.properties.accessToken").exists())
			.andExpect(jsonPath("$.paths['/actuator/health'].get").exists());
	}

	@Test
	void bearerSchemeAppliesOnlyToProtectedEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
			.andExpect(jsonPath(CARD_OP + ".security[0].bearerAuth").exists())
			.andExpect(jsonPath(TOKEN_OP + ".security").doesNotExist())
			.andExpect(jsonPath("$.paths['/actuator/health'].get.security").doesNotExist());
	}

	@Test
	void swaggerUiIsServed() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/swagger-ui/index.html"));
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
	}

	@Test
	void docsPathsDoNotOpenOtherMethods() throws Exception {
		mockMvc.perform(post("/v3/api-docs"))
			.andExpect(status().isUnauthorized());
	}

}
