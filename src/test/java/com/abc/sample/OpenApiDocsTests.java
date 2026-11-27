package com.abc.sample;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocsDescribeAllEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.info.title").value("Sample API"))
			.andExpect(jsonPath("$.paths['/api/cards/validate'].post.summary").value("Validate a card number"))
			.andExpect(jsonPath("$.paths['/api/cards/validate'].post.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/cards/validate'].post.responses['400']").exists())
			.andExpect(jsonPath("$.components.schemas.CardValidationRequest.required", hasItem("cardNumber")))
			.andExpect(jsonPath("$.components.schemas.CardValidationResponse.properties.valid").exists())
			.andExpect(jsonPath("$.paths['/actuator/health'].get").exists());
	}

	@Test
	void swaggerUiIsServed() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/swagger-ui/index.html"));
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

}
