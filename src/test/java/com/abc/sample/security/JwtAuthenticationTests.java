package com.abc.sample.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationTests {

	private static final String TOKEN_URL = "/api/auth/token";

	private static final String PROTECTED_URL = "/api/cards/validate";

	private static final String CARD_BODY = "{\"cardNumber\":\"4111111111111111\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProperties jwtProperties;

	private ResultActions requestToken(String body) throws Exception {
		return mockMvc.perform(post(TOKEN_URL).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String obtainToken(String username, String password) throws Exception {
		String json = requestToken("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(json, "$.accessToken");
	}

	private ResultActions callProtected(String bearerToken) throws Exception {
		return mockMvc.perform(post(PROTECTED_URL).contentType(MediaType.APPLICATION_JSON)
			.content(CARD_BODY)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken));
	}

	private String signToken(byte[] key, String issuer, Instant issuedAt, Instant expiresAt) {
		NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.subject("tester")
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.claim(SecurityConfig.ROLES_CLAIM, List.of("USER"))
			.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	private byte[] configuredKey() {
		return jwtProperties.secretKey().getEncoded();
	}

	// --- token endpoint ---

	@Test
	void validCredentialsReturnToken() throws Exception {
		requestToken("{\"username\":\"tester\",\"password\":\"tester-password\"}").andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900));
	}

	@Test
	void wrongPasswordIsUnauthorized() throws Exception {
		requestToken("{\"username\":\"tester\",\"password\":\"wrong\"}").andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.detail").value("Invalid credentials"))
			.andExpect(content().string(not(containsString("accessToken"))));
	}

	@Test
	void unknownUserIsUnauthorized() throws Exception {
		requestToken("{\"username\":\"nobody\",\"password\":\"tester-password\"}")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void unknownUserAndWrongPasswordGiveIdenticalBodies() throws Exception {
		String wrongPassword = requestToken("{\"username\":\"tester\",\"password\":\"wrong\"}").andReturn()
			.getResponse()
			.getContentAsString();
		String unknownUser = requestToken("{\"username\":\"nobody\",\"password\":\"wrong\"}").andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(unknownUser).isEqualTo(wrongPassword);
	}

	@Test
	void failedLoginDoesNotEchoPassword() throws Exception {
		requestToken("{\"username\":\"tester\",\"password\":\"s3cret-attempt\"}")
			.andExpect(status().isUnauthorized())
			.andExpect(content().string(not(containsString("s3cret-attempt"))));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"username\":\"tester\"}", "{\"password\":\"tester-password\"}",
			"{\"username\":\" \",\"password\":\"tester-password\"}", "{\"username\":\"tester\",\"password\":\"\"}" })
	void missingCredentialsAreBadRequest(String body) throws Exception {
		requestToken(body).andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Request validation failed"))
			.andExpect(jsonPath("$.errors").isNotEmpty())
			.andExpect(content().string(not(containsString("tester-password"))));
	}

	@Test
	void tokenEndpointRejectsGet() throws Exception {
		// Only POST is open; a GET is treated as a protected request.
		mockMvc.perform(get(TOKEN_URL)).andExpect(status().isUnauthorized());
	}

	// --- protected endpoints ---

	@Test
	void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(post(PROTECTED_URL).contentType(MediaType.APPLICATION_JSON).content(CARD_BODY))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")));
	}

	@Test
	void protectedEndpointWithIssuedTokenSucceeds() throws Exception {
		callProtected(obtainToken("tester", "tester-password")).andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true));
	}

	@Test
	void malformedTokenIsUnauthorized() throws Exception {
		callProtected("not-a-jwt").andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenIsUnauthorized() throws Exception {
		Instant past = Instant.now().minusSeconds(3600);
		String token = signToken(configuredKey(), jwtProperties.issuer(), past, past.plusSeconds(60));

		callProtected(token).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenFromAnotherIssuerIsUnauthorized() throws Exception {
		Instant now = Instant.now();
		String token = signToken(configuredKey(), "someone-else", now, now.plusSeconds(300));

		callProtected(token).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSignedWithAnotherKeyIsUnauthorized() throws Exception {
		Instant now = Instant.now();
		byte[] otherKey = "a-completely-different-32-byte-key!!".getBytes(StandardCharsets.UTF_8);
		String token = signToken(otherKey, jwtProperties.issuer(), now, now.plusSeconds(300));

		callProtected(token).andExpect(status().isUnauthorized());
	}

	@Test
	void unsignedTokenIsUnauthorized() throws Exception {
		Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
		long exp = Instant.now().plusSeconds(300).getEpochSecond();
		String header = b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
		String payload = b64.encodeToString(("{\"sub\":\"tester\",\"iss\":\"" + jwtProperties.issuer()
				+ "\",\"exp\":" + exp + ",\"roles\":[\"USER\"]}")
			.getBytes(StandardCharsets.UTF_8));

		callProtected(header + "." + payload + ".").andExpect(status().isUnauthorized());
	}

	// --- open endpoints ---

	@Test
	void healthIsOpenWithoutToken() throws Exception {
		mockMvc.perform(get("/actuator/health").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void healthProbesAreOpenWithoutToken() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	void noSessionCookieIsIssued() throws Exception {
		requestToken("{\"username\":\"tester\",\"password\":\"tester-password\"}")
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
	}

}
