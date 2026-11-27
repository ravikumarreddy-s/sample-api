package com.abc.sample.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Card number to validate")
public record CardValidationRequest(
		@Schema(description = "Card number, 12 to 19 digits. Spaces and hyphens are allowed as separators.",
				example = "4111 1111 1111 1111", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank String cardNumber) {
}
