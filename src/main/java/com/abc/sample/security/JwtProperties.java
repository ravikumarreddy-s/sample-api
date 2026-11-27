package com.abc.sample.security;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT settings, bound from {@code app.security.jwt.*}.
 *
 * @param secret base64-encoded HMAC key; must decode to at least 256 bits (HS256)
 * @param issuer value written to and required in the {@code iss} claim
 * @param ttl how long an issued access token stays valid
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, @DefaultValue("sample-api") String issuer,
		@DefaultValue("15m") Duration ttl) {

	static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException(
					"app.security.jwt.secret must be set (e.g. via the JWT_SECRET environment variable)");
		}
		byte[] key;
		try {
			key = Base64.getDecoder().decode(secret.trim());
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("app.security.jwt.secret must be base64-encoded", ex);
		}
		if (key.length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"app.security.jwt.secret must decode to at least " + MIN_SECRET_BYTES + " bytes");
		}
		if (ttl == null || ttl.isNegative() || ttl.isZero()) {
			throw new IllegalArgumentException("app.security.jwt.ttl must be positive");
		}
	}

	SecretKey secretKey() {
		return new SecretKeySpec(Base64.getDecoder().decode(secret.trim()), "HmacSHA256");
	}

	/** Keep the key out of logs and failure-analysis output. */
	@Override
	public String toString() {
		return "JwtProperties[secret=****, issuer=" + issuer + ", ttl=" + ttl + "]";
	}

}
