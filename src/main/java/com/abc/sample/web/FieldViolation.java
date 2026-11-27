package com.abc.sample.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One failed field in a validation error. Holds the field name and the constraint message
 * only: the rejected value is never included, so sensitive input (card numbers, passwords)
 * is not echoed back.
 */
@Schema(description = "A request field that failed validation")
public record FieldViolation(
		@Schema(description = "Name of the invalid field", example = "cardNumber") String field,
		@Schema(description = "Why the value was rejected", example = "must not be blank") String message) {
}
