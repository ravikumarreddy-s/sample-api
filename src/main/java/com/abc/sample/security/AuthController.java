package com.abc.sample.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Exchanges a username and password for a JWT access token.
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
			content = @Content)
	@ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
	@PostMapping(TOKEN_PATH)
	public TokenResponse token(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Account credentials",
					required = true) @RequestBody TokenRequest request) {
		if (isBlank(request.username()) || isBlank(request.password())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
		}
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
		}
		catch (AuthenticationException ex) {
			// Same response for an unknown user and a wrong password: don't reveal which.
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
		}
		return tokenService.issue(authentication);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
