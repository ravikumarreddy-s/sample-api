package com.abc.sample.card;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LuhnValidatorTests {

	private final LuhnValidator validator = new LuhnValidator();

	@ParameterizedTest
	@ValueSource(strings = {
			"4111111111111111", // Visa
			"4012888888881881", // Visa
			"5555555555554444", // Mastercard
			"378282246310005", // American Express
			"6011111111111117", // Discover
			"3530111333300000", // JCB
			"4111 1111 1111 1111", // spaces
			"4111-1111-1111-1111", // hyphens
			"000000000000" // 12 digits, sum 0
	})
	void acceptsNumbersWithValidChecksum(String cardNumber) {
		assertThat(validator.isValid(cardNumber)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"4111111111111112", // last digit off by one
			"5555555555554440",
			"378282246310006",
			"4111111111111121" // two adjacent digits swapped
	})
	void rejectsNumbersWithInvalidChecksum(String cardNumber) {
		assertThat(validator.isValid(cardNumber)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"4111a11111111111", // letter
			"4111.1111.1111.1111", // unsupported separator
			"４１１１１１１１１１１１１１１１" // full-width digits
	})
	void rejectsNonDigitCharacters(String cardNumber) {
		assertThat(validator.isValid(cardNumber)).isFalse();
	}

	@Test
	void rejectsNumbersShorterThanMinimumLength() {
		// 11 digits that would pass the checksum
		assertThat(validator.isValid("00000000000")).isFalse();
	}

	@Test
	void rejectsNumbersLongerThanMaximumLength() {
		// 20 digits that would pass the checksum
		assertThat(validator.isValid("00000000000000000000")).isFalse();
	}

	@Test
	void acceptsMaximumLengthNumber() {
		assertThat(validator.isValid("0000000000000000000")).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "- - -" })
	void rejectsMissingOrSeparatorOnlyInput(String cardNumber) {
		assertThat(validator.isValid(cardNumber)).isFalse();
	}

}
