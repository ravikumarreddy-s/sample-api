package com.abc.sample.security;

import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.abc.sample.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Exchanges a username and password for a JWT access token. A failed login throws an
 * {@code AuthenticationException}, which {@code GlobalExceptionHandler} maps to 401 with the
 * same response for an unknown user and a wrong password.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Obtain JWT access tokens")
public class AuthController {

	static final String TOKEN_PATH = "/api/auth/token";

	private final AuthenticationManager authenticationManager;

	private final TokenService tokenService;

	@Operation(summary = "Get an access token",
			description = "Exchanges a username and password for a signed JWT. No token is needed to call this.")
	@ApiResponse(responseCode = "200", description = "Token issued")
	@ApiResponse(responseCode = "400", description = "`username` or `password` is missing or blank",
			content = @Content(mediaType = OpenApiConfig.PROBLEM_JSON,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Invalid credentials",
			content = @Content(mediaType = OpenApiConfig.PROBLEM_JSON,
					schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping(TOKEN_PATH)
	public TokenResponse token(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Account credentials",
					required = true) @Valid @RequestBody TokenRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
		return tokenService.issue(authentication);
	}

}
