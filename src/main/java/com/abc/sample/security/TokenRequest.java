package com.abc.sample.security;

public record TokenRequest(String username, String password) {

	/** Never include the password in logs or error messages. */
	@Override
	public String toString() {
		return "TokenRequest[username=" + username + ", password=****]";
	}

}
