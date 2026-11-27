package com.abc.sample.card;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abc.sample.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Validates card numbers with the Luhn algorithm. The number is taken in a POST body,
 * not the URL, so it doesn't end up in access logs, and it is never echoed back.
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Credit card number validation")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CardValidationController {

	private final LuhnValidator luhnValidator;

	@Operation(summary = "Validate a card number",
			description = "Checks the card number with the Luhn (mod 10) algorithm. Spaces and hyphens are "
					+ "allowed as separators; after removing them the number must be 12 to 19 digits. "
					+ "The number is never echoed back in the response.")
	@ApiResponse(responseCode = "200", description = "Check completed; `valid` holds the result")
	@ApiResponse(responseCode = "400", description = "`cardNumber` is missing or blank, or the body is not valid JSON",
			content = @Content(mediaType = OpenApiConfig.PROBLEM_JSON,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired bearer token", content = @Content)
	@PostMapping("/validate")
	public CardValidationResponse validate(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Card number to validate",
					required = true) @Valid @RequestBody CardValidationRequest request) {
		return new CardValidationResponse(luhnValidator.isValid(request.cardNumber()));
	}

}
