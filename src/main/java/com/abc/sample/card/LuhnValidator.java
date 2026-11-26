package com.abc.sample.card;

import org.springframework.stereotype.Component;

/**
 * Checks card numbers with the Luhn (mod 10) algorithm.
 *
 * <p>Spaces and hyphens are accepted as separators. After removing them, the number
 * must be 12 to 19 digits long (the range used by payment card numbers).
 */
@Component
public class LuhnValidator {

	static final int MIN_LENGTH = 12;

	static final int MAX_LENGTH = 19;

	public boolean isValid(String cardNumber) {
		if (cardNumber == null) {
			return false;
		}
		String digits = cardNumber.replaceAll("[ -]", "");
		if (digits.length() < MIN_LENGTH || digits.length() > MAX_LENGTH) {
			return false;
		}

		int sum = 0;
		boolean doubleIt = false;
		// Walk from the rightmost (check) digit; double every second digit.
		for (int i = digits.length() - 1; i >= 0; i--) {
			char c = digits.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
			int digit = c - '0';
			if (doubleIt) {
				digit *= 2;
				if (digit > 9) {
					digit -= 9;
				}
			}
			sum += digit;
			doubleIt = !doubleIt;
		}
		return sum % 10 == 0;
	}

}
