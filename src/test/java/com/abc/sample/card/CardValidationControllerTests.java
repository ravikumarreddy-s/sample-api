package com.abc.sample.card;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(CardValidationController.class)
@Import(LuhnValidator.class)
class CardValidationControllerTests {

	private static final String URL = "/api/cards/validate";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postJson(String body) throws Exception {
		return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void validNumberReturnsTrue() throws Exception {
		postJson("{\"cardNumber\":\"4111 1111 1111 1111\"}")
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.valid").value(true));
	}

	@Test
	void invalidNumberReturnsFalse() throws Exception {
		postJson("{\"cardNumber\":\"4111111111111112\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false));
	}

	@Test
	void malformedNumberReturnsFalse() throws Exception {
		postJson("{\"cardNumber\":\"not-a-card\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false));
	}

	@Test
	void responseDoesNotEchoCardNumber() throws Exception {
		postJson("{\"cardNumber\":\"4111111111111111\"}")
			.andExpect(content().string(not(containsString("4111111111111111"))));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"cardNumber\":null}", "{\"cardNumber\":\"\"}", "{\"cardNumber\":\"   \"}" })
	void missingOrBlankNumberReturnsBadRequest(String body) throws Exception {
		postJson(body).andExpect(status().isBadRequest());
	}

	@Test
	void malformedJsonReturnsBadRequest() throws Exception {
		postJson("{\"cardNumber\":").andExpect(status().isBadRequest());
	}

	@Test
	void getIsNotAllowed() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isMethodNotAllowed());
	}

}
