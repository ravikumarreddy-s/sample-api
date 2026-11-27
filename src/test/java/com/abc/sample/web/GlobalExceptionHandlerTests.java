package com.abc.sample.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.abc.sample.security.SecurityConfig;

@WebMvcTest(GlobalExceptionHandlerTests.ThrowingController.class)
@Import({ GlobalExceptionHandlerTests.ThrowingController.class, SecurityConfig.class })
@ActiveProfiles("test")
class GlobalExceptionHandlerTests {

	private static final String INTERNAL_MESSAGE = "db password is hunter2";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unexpectedExceptionReturnsGenericServerError() throws Exception {
		mockMvc.perform(get("/test/boom").with(jwt()))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.title").value("Internal Server Error"))
			.andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
			.andExpect(jsonPath("$.instance").value("/test/boom"))
			.andExpect(content().string(not(containsString(INTERNAL_MESSAGE))));
	}

	@Test
	void accessDeniedReturnsForbidden() throws Exception {
		mockMvc.perform(get("/test/denied").with(jwt()))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Access denied"));
	}

	@Test
	void responseStatusExceptionKeepsItsStatusAndReason() throws Exception {
		mockMvc.perform(get("/test/conflict").with(jwt()))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("already exists"));
	}

	@Test
	void unknownPathReturnsNotFoundProblem() throws Exception {
		mockMvc.perform(get("/test/does-not-exist").with(jwt()))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404));
	}

	@RestController
	static class ThrowingController {

		@GetMapping("/test/boom")
		String boom() {
			throw new IllegalStateException(INTERNAL_MESSAGE);
		}

		@GetMapping("/test/denied")
		String denied() {
			throw new AccessDeniedException("no");
		}

		@GetMapping("/test/conflict")
		String conflict() {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "already exists");
		}

	}

}
