package com.abc.sample.card;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of the card number check")
public record CardValidationResponse(
		@Schema(description = "Whether the number is 12 to 19 digits and passes the Luhn check",
				example = "true") boolean valid) {
}
