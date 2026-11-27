package com.abc.sample.logging;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs entry and exit (with elapsed time) of public methods on the application's
 * {@code @RestController}, {@code @Service} and {@code @Component} beans, at DEBUG level:
 *
 * <pre>
 * --&gt; CardValidationController.validate()
 * &lt;-- CardValidationController.validate() 3 ms
 * &lt;-- AuthController.token() threw BadCredentialsException after 85 ms
 * </pre>
 *
 * <p>Only class and method names are logged: never argument values, return values or exception
 * messages, which can hold card numbers, passwords or tokens. Enable with
 * {@code logging.level.com.abc.sample.logging: DEBUG} (on in the {@code local} profile).
 */
@Aspect
@Component
public class MethodLoggingAspect {

	private static final Logger log = LoggerFactory.getLogger(MethodLoggingAspect.class);

	/** Application classes only. Spring AOP never proxies {@code @Aspect} beans, so this aspect is not advised. */
	@Pointcut("within(com.abc.sample..*)")
	void applicationCode() {
	}

	@Pointcut("@within(org.springframework.web.bind.annotation.RestController)"
			+ " || @within(org.springframework.stereotype.Service)"
			+ " || @within(org.springframework.stereotype.Component)")
	void applicationBean() {
	}

	@Around("execution(public * *(..)) && applicationCode() && applicationBean()")
	public Object logEntryAndExit(ProceedingJoinPoint joinPoint) throws Throwable {
		if (!log.isDebugEnabled()) {
			return joinPoint.proceed();
		}
		String method = joinPoint.getSignature().getDeclaringType().getSimpleName() + "."
				+ joinPoint.getSignature().getName() + "()";
		log.debug("--> {}", method);
		long start = System.nanoTime();
		try {
			Object result = joinPoint.proceed();
			log.debug("<-- {} {} ms", method, elapsedMillis(start));
			return result;
		}
		catch (Throwable ex) {
			log.debug("<-- {} threw {} after {} ms", method, ex.getClass().getSimpleName(), elapsedMillis(start));
			throw ex;
		}
	}

	private static long elapsedMillis(long startNanos) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
	}

}
