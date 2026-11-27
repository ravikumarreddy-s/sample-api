package com.abc.sample.security;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
