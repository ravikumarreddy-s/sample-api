package com.abc.sample.security;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Accounts that may request tokens, bound from {@code app.security.users.<username>.*}.
 * Passwords are stored encoded with an id prefix, e.g. {@code {bcrypt}$2a$10$...}.
 */
@ConfigurationProperties("app.security")
public record UserAccountProperties(@DefaultValue Map<String, Account> users) {

	public record Account(String password, @DefaultValue("USER") List<String> roles) {

		@Override
		public String toString() {
			return "Account[password=****, roles=" + roles + "]";
		}

	}

}
