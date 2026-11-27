package com.abc.sample.security;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issued access token")
public record TokenResponse(
		@Schema(description = "Signed JWT (HS256). Send it as `Authorization: Bearer <token>`.") String accessToken,
		@Schema(description = "Always `Bearer`", example = "Bearer") String tokenType,
		@Schema(description = "Seconds until the token expires", example = "900") long expiresIn) {
}
