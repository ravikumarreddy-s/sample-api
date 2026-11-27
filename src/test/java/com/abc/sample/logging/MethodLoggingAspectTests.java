package com.abc.sample.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@ExtendWith(OutputCaptureExtension.class)
class MethodLoggingAspectTests {

	private static final String SECRET_ARG = "4111111111111111";

	private final Logger aspectLogger = (Logger) LoggerFactory.getLogger(MethodLoggingAspect.class);

	private Level originalLevel;

	@BeforeEach
	void enableDebug() {
		originalLevel = aspectLogger.getLevel();
		aspectLogger.setLevel(Level.DEBUG);
	}

	@AfterEach
	void restoreLevel() {
		aspectLogger.setLevel(originalLevel);
	}

	private static SampleBean proxy() {
		AspectJProxyFactory factory = new AspectJProxyFactory(new SampleBean());
		factory.setProxyTargetClass(true);
		factory.addAspect(new MethodLoggingAspect());
		return factory.getProxy();
	}

	@Test
	void logsEntryAndExitWithoutArgumentsOrResult(CapturedOutput output) {
		String result = proxy().echo(SECRET_ARG);

		assertThat(result).isEqualTo(SECRET_ARG);
		assertThat(output).contains("--> SampleBean.echo()")
			.containsPattern("<-- SampleBean\\.echo\\(\\) \\d+ ms")
			.doesNotContain(SECRET_ARG);
	}

	@Test
	void logsExceptionTypeButNotMessageAndRethrows(CapturedOutput output) {
		assertThatIllegalStateException().isThrownBy(() -> proxy().fail(SECRET_ARG)).withMessage(SECRET_ARG);

		assertThat(output).contains("--> SampleBean.fail()")
			.containsPattern("<-- SampleBean\\.fail\\(\\) threw IllegalStateException after \\d+ ms")
			.doesNotContain(SECRET_ARG);
	}

	@Test
	void logsNothingWhenDebugIsOff(CapturedOutput output) {
		aspectLogger.setLevel(Level.INFO);

		assertThat(proxy().echo("x")).isEqualTo("x");
		assertThat(output).doesNotContain("SampleBean.echo()");
	}

	@Component
	static class SampleBean {

		public String echo(String value) {
			return value;
		}

		public String fail(String message) {
			throw new IllegalStateException(message);
		}

	}

}
