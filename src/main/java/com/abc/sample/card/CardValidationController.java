package com.abc.sample.card;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * Validates card numbers with the Luhn algorithm. The number is taken in a POST body,
 * not the URL, so it doesn't end up in access logs, and it is never echoed back.
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardValidationController {

	private final LuhnValidator luhnValidator;

	@PostMapping("/validate")
	public CardValidationResponse validate(@RequestBody CardValidationRequest request) {
		if (request.cardNumber() == null || request.cardNumber().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cardNumber is required");
		}
		return new CardValidationResponse(luhnValidator.isValid(request.cardNumber()));
	}

}
