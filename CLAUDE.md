# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`sample-api` is a Spring Boot 4.2 REST API skeleton (Java 17, Gradle 9.9 wrapper, base package `com.abc.sample`). It has no domain code yet: just the application class, Actuator health and tests.

## Commands

```bash
./gradlew build                      # compile + test + package
./gradlew test                       # run all tests
./gradlew test --tests 'com.abc.sample.HealthEndpointTests'                    # one test class
./gradlew test --tests 'com.abc.sample.HealthEndpointTests.healthReturnsUp'    # one test method
./gradlew bootRun                    # start the app on :8080
curl -H 'Accept: application/json' localhost:8080/actuator/health
```

There is no linter or formatter configured.

## Architecture notes

- **Spring Boot 4 uses modular starters and test packages.** The web starter is `spring-boot-starter-webmvc` (not `-web`), and its test starter is `spring-boot-starter-webmvc-test`. Test annotations moved too: use `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `AutoConfigureMockMvc`, not the Boot 3 `org.springframework.boot.test.autoconfigure.web.servlet` package.
- **Health comes from Spring Boot Actuator, not a custom controller.** `GET /actuator/health` (plus `/liveness` and `/readiness` groups). Only the health endpoint is web-exposed (Actuator default) and `show-details` is `never`. Keep it that way unless explicitly asked; don't expose `env`, `heapdump`, etc.
- Actuator returns `application/vnd.spring-boot.actuator.v3+json` unless the request sends `Accept: application/json`; tests asserting a JSON content type must set that header.
- Actuator endpoints aren't loaded by `@WebMvcTest` slices, so endpoint tests use `@SpringBootTest` + `@AutoConfigureMockMvc`.
- Lombok is on the compile and test classpaths.
- Config lives in `src/main/resources/application.yaml` (YAML, not `.properties`).

## Git workflow

- Remote: `github.com/ravikumarreddy-s/sample-api`. `gh` CLI is installed and logged in.
- Work on a feature branch, push it and open a PR into `main`. **Never merge PRs**: the owner reviews and merges them.
