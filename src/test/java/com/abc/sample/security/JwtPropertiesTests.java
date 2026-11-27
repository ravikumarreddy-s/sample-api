package com.abc.sample.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtPropertiesTests {

	private static final String VALID_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

	@Test
	void acceptsValidSettings() {
		JwtProperties properties = new JwtProperties(VALID_SECRET, "issuer", Duration.ofMinutes(5));

		assertThat(properties.secretKey().getEncoded()).hasSize(32);
		assertThat(properties.secretKey().getAlgorithm()).isEqualTo("HmacSHA256");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void rejectsMissingSecret(String secret) {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(secret, "issuer", Duration.ofMinutes(5)))
			.withMessageContaining("must be set");
	}

	@Test
	void rejectsSecretThatIsNotBase64() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JwtProperties("not base64!!", "issuer", Duration.ofMinutes(5)))
			.withMessageContaining("base64");
	}

	@Test
	void rejectsSecretShorterThan256Bits() {
		String shortSecret = Base64.getEncoder().encodeToString(new byte[31]);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JwtProperties(shortSecret, "issuer", Duration.ofMinutes(5)))
			.withMessageContaining("at least 32 bytes");
	}

	@Test
	void rejectsNonPositiveTtl() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(VALID_SECRET, "issuer", Duration.ZERO));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JwtProperties(VALID_SECRET, "issuer", Duration.ofSeconds(-1)));
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(VALID_SECRET, "issuer", null));
	}

	@Test
	void toStringDoesNotRevealSecret() {
		JwtProperties properties = new JwtProperties(VALID_SECRET, "issuer", Duration.ofMinutes(5));

		assertThat(properties.toString()).doesNotContain(VALID_SECRET).contains("issuer");
	}

}
