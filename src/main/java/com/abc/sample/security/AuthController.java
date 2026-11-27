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

import lombok.RequiredArgsConstructor;

/**
 * Exchanges a username and password for a JWT access token.
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

	static final String TOKEN_PATH = "/api/auth/token";

	private final AuthenticationManager authenticationManager;

	private final TokenService tokenService;

	@PostMapping(TOKEN_PATH)
	public TokenResponse token(@RequestBody TokenRequest request) {
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
