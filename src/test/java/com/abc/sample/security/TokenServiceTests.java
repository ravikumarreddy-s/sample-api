package com.abc.sample.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class TokenServiceTests {

	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	private final JwtProperties properties = new JwtProperties(
			Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()), "test-issuer",
			Duration.ofMinutes(10));

	private final SecurityConfig config = new SecurityConfig();

	private final TokenService tokenService = new TokenService(config.jwtEncoder(properties), properties,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void issuesSignedTokenWithExpectedClaims() {
		var authentication = UsernamePasswordAuthenticationToken.authenticated("alice", null,
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN", "SCOPE_ignored"));

		TokenResponse response = tokenService.issue(authentication);

		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(600);

		// Decode without time validation: the token was issued at a fixed past instant.
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey()).build();
		decoder.setJwtValidator((jwt) -> OAuth2TokenValidatorResult.success());
		Jwt jwt = decoder.decode(response.accessToken());

		assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
		assertThat(jwt.getSubject()).isEqualTo("alice");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("test-issuer");
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
		assertThat(jwt.getClaimAsStringList(SecurityConfig.ROLES_CLAIM)).isEqualTo(List.of("USER", "ADMIN"));
	}

	@Test
	void tokenIssuedNowPassesConfiguredDecoder() {
		TokenService liveService = new TokenService(config.jwtEncoder(properties), properties);
		var authentication = UsernamePasswordAuthenticationToken.authenticated("bob", null,
				AuthorityUtils.createAuthorityList("ROLE_USER"));

		Jwt jwt = config.jwtDecoder(properties).decode(liveService.issue(authentication).accessToken());

		assertThat(jwt.getSubject()).isEqualTo("bob");
	}

	@Test
	void rolesClaimBecomesRoleAuthorities() {
		TokenService liveService = new TokenService(config.jwtEncoder(properties), properties);
		var authentication = UsernamePasswordAuthenticationToken.authenticated("carol", null,
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN"));
		Jwt jwt = config.jwtDecoder(properties).decode(liveService.issue(authentication).accessToken());

		var converted = config.jwtAuthenticationConverter().convert(jwt);

		assertThat(AuthorityUtils.authorityListToSet(converted.getAuthorities()))
			.contains("ROLE_USER", "ROLE_ADMIN");
		assertThat(converted.getName()).isEqualTo("carol");
	}

}
