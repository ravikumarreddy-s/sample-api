package com.abc.sample.web;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns exceptions thrown by controllers into RFC 9457 problem details
 * ({@code application/problem+json}) with {@code type}, {@code title}, {@code status},
 * {@code detail} and {@code instance} (the request path).
 *
 * <p>Spring MVC exceptions (malformed JSON, unsupported method or media type, unknown path,
 * {@code ResponseStatusException}, ...) are handled by {@link ResponseEntityExceptionHandler}.
 * Validation failures add an {@value #ERRORS_PROPERTY} list of {@link FieldViolation}s.
 *
 * <p>Error responses never echo request values, and {@code detail} never carries internal
 * exception messages. Errors raised by the security filter chain (a missing or invalid bearer
 * token) happen before Spring MVC and are not handled here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	static final String ERRORS_PROPERTY = "errors";

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> violations = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map((error) -> new FieldViolation(error.getField(), error.getDefaultMessage()))
			.toList();
		ProblemDetail body = ex.getBody();
		body.setDetail("Request validation failed");
		body.setProperty(ERRORS_PROPERTY, violations);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	/** Failed login at the token endpoint. Same response for an unknown user and a wrong password. */
	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", request.getRequestURI());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return problem(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
	}

	/** Anything unexpected: log it server-side and return a generic 500 with no internals. */
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception while processing request", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI());
	}

	/** Every response built by the base class passes through here; fill in {@code instance}. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (body instanceof ProblemDetail problem && problem.getInstance() == null
				&& request instanceof ServletWebRequest servletRequest) {
			problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	private static ProblemDetail problem(HttpStatus status, String detail, String path) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setInstance(URI.create(path));
		return problem;
	}

}
