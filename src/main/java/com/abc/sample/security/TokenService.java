package com.abc.sample.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues signed access tokens for authenticated users.
 */
@Service
public class TokenService {

	private static final String ROLE_PREFIX = "ROLE_";

	private final JwtEncoder jwtEncoder;

	private final JwtProperties properties;

	private final Clock clock;

	@Autowired
	public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
		this(jwtEncoder, properties, Clock.systemUTC());
	}

	TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	public TokenResponse issue(Authentication authentication) {
		Instant now = clock.instant();
		List<String> roles = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.filter((authority) -> authority != null && authority.startsWith(ROLE_PREFIX))
			.map((authority) -> authority.substring(ROLE_PREFIX.length()))
			.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(authentication.getName())
			.issuedAt(now)
			.expiresAt(now.plus(properties.ttl()))
			.claim(SecurityConfig.ROLES_CLAIM, roles)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", properties.ttl().toSeconds());
	}

}
