# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`sample-api` is a Spring Boot 4.2 REST API skeleton (Java 17, Gradle 9.9 wrapper, base package `com.abc.sample`). Besides Actuator health, it has one feature: Luhn credit-card validation (`com.abc.sample.card`), protected by JWT authentication (`com.abc.sample.security`).

## Commands

```bash
./gradlew build                      # compile + test + package
./gradlew test                       # run all tests
./gradlew test --tests 'com.abc.sample.HealthEndpointTests'                    # one test class
./gradlew test --tests 'com.abc.sample.HealthEndpointTests.healthReturnsUp'    # one test method
./gradlew check                      # tests + JaCoCo coverage gate (80% line and branch)
# start the app on :8080 with the `local` profile (application-local.yaml: dev-only signing key + demo/demo-pass user)
./gradlew bootRun --args='--spring.profiles.active=local'
curl -H 'Accept: application/json' localhost:8080/actuator/health
TOKEN=$(curl -s -H "Content-Type: application/json" -d '{"username":"demo","password":"demo-pass"}' localhost:8080/api/auth/token | jq -r .accessToken)
curl -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"cardNumber":"4111 1111 1111 1111"}' localhost:8080/api/cards/validate
```

There is no linter or formatter configured. Coverage report: `build/reports/jacoco/test/html/index.html`.

## Architecture notes

- **Spring Boot 4 uses modular starters and test packages.** The web starter is `spring-boot-starter-webmvc` (not `-web`), and its test starter is `spring-boot-starter-webmvc-test`. Test annotations moved too: use `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `AutoConfigureMockMvc`, not the Boot 3 `org.springframework.boot.test.autoconfigure.web.servlet` package.
- **Health comes from Spring Boot Actuator, not a custom controller.** `GET /actuator/health` (plus `/liveness` and `/readiness` groups). Only the health endpoint is web-exposed (Actuator default) and `show-details` is `never`. Keep it that way unless explicitly asked; don't expose `env`, `heapdump`, etc.
- Actuator returns `application/vnd.spring-boot.actuator.v3+json` unless the request sends `Accept: application/json`; tests asserting a JSON content type must set that header.
- Actuator endpoints aren't loaded by `@WebMvcTest` slices, so endpoint tests use `@SpringBootTest` + `@AutoConfigureMockMvc`.
- Lombok is on the compile and test classpaths.
- **Card validation:** `POST /api/cards/validate` with `{"cardNumber": "..."}` returns `{"valid": true|false}`. Spaces and hyphens are allowed as separators; 12 to 19 digits. Missing or blank `cardNumber` returns 400. The number goes in the body (not the URL) and is never echoed back or logged; keep it that way.
- **Security (`com.abc.sample.security`):** stateless JWT via Spring Security's OAuth2 resource server, HS256 tokens signed with `app.security.jwt.secret` (base64, ≥ 256 bits, from `JWT_SECRET`; startup fails without it). `POST /api/auth/token` with `{"username","password"}` returns `{"accessToken","tokenType":"Bearer","expiresIn"}`; bad credentials → 401 (same for unknown user and wrong password), missing fields → 400. Accounts come from `app.security.users.<name>.{password,roles}` with id-prefixed encoded passwords (`{bcrypt}...`); none are configured by default. Tokens carry `iss`, `sub`, `iat`, `exp` and a `roles` claim mapped to `ROLE_*` authorities. Open without a token: `GET /actuator/health/**`, `POST /api/auth/token`, `/error`; everything else needs `Authorization: Bearer`. CSRF, sessions, form login and HTTP Basic are off. Never log passwords, secrets or tokens.
- **Errors (`com.abc.sample.web.GlobalExceptionHandler`, a `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`):** controller errors are RFC 9457 problem details (`application/problem+json`: `type`, `title`, `status`, `detail`, `instance`). Request DTOs use Bean Validation (`@NotBlank` + `@Valid`); failures add an `errors` list of `{field, message}` and never include the rejected value. `AuthenticationException` → 401 "Invalid credentials", `AccessDeniedException` → 403, anything else → logged and a generic 500. Throw exceptions from controllers rather than building error responses there; document error responses with `@Schema(implementation = ProblemDetail.class)` under `OpenApiConfig.PROBLEM_JSON`. The security filter chain's 401 for a missing or invalid bearer token happens before MVC and still has an empty body.
- **Tests run with the `test` profile** (`src/test/resources/application-test.yaml`: test signing key plus `tester`/`admin` users). Every `@SpringBootTest`/`@WebMvcTest` class needs `@ActiveProfiles("test")`; `@WebMvcTest` slices must `@Import(SecurityConfig.class)` and use `SecurityMockMvcRequestPostProcessors.jwt()` for authenticated requests. Spring Security 7 adds a `FACTOR_BEARER` authority to JWT authentications, so don't assert an exact authority set.
- **API docs (springdoc-openapi 3.x):** OpenAPI JSON at `GET /v3/api-docs`, Swagger UI at `/swagger-ui.html`; both are open (GET only) in `SecurityConfig`. `springdoc.show-actuator: true` includes the health endpoint. `config/OpenApiConfig` holds the API metadata and the `bearerAuth` (HTTP bearer, JWT) scheme. Document new endpoints with `@Tag`/`@Operation`/`@ApiResponse` and `@Schema` on request/response records, and put `@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)` on protected controllers (not on open ones like the token endpoint). `OpenApiDocsTests` checks the generated document.
- Config lives in `src/main/resources/application.yaml` (YAML, not `.properties`). `application-local.yaml` is a dev-only profile with a public signing key (`JWT_SECRET` still overrides it) and a `demo` user; never activate it outside local development.

## Git workflow

- Remote: `github.com/ravikumarreddy-s/sample-api`. `gh` CLI is installed and logged in.
- Work on a feature branch, push it and open a PR into `main`. **Never merge PRs**: the owner reviews and merges them.

## Claude Code hooks

`.claude/settings.json` registers hooks (scripts in `.claude/hooks/`) that enforce the rules above:

- `block-pr-merge.sh` blocks any `gh pr merge` command.
- `protect-main.sh` blocks `git commit`/`git push` while on `main`, and pushes targeting `main`. Deleting another remote branch (`git push origin --delete <branch>`) is allowed from `main`. It checks the branch *before* the command runs, so create a feature branch in a separate command, not chained with `&& git commit`.
- `run-tests.sh` runs `./gradlew test` after any `.java` file is edited and reports failures back.
