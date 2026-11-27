package com.abc.sample.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials to exchange for an access token")
public record TokenRequest(
		@Schema(description = "Account username", example = "demo",
				requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String username,
		@Schema(description = "Account password", format = "password",
				requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String password) {

	/** Never include the password in logs or error messages. */
	@Override
	public String toString() {
		return "TokenRequest[username=" + username + ", password=****]";
	}

}
