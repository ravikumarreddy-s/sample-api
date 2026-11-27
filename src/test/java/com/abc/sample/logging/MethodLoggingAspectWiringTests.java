package com.abc.sample.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.abc.sample.card.CardValidationController;
import com.abc.sample.card.LuhnValidator;
import com.abc.sample.config.OpenApiConfig;
import com.abc.sample.security.AuthController;
import com.abc.sample.security.TokenService;
import com.abc.sample.web.GlobalExceptionHandler;

/** Checks which application beans the aspect applies to. */
@SpringBootTest
@ActiveProfiles("test")
class MethodLoggingAspectWiringTests {

	@Autowired
	private ApplicationContext context;

	private boolean isAdvised(Class<?> type) {
		return AopUtils.isAopProxy(context.getBean(type));
	}

	@Test
	void controllersAndServicesAreAdvised() {
		assertThat(isAdvised(CardValidationController.class)).isTrue();
		assertThat(isAdvised(AuthController.class)).isTrue();
		assertThat(isAdvised(TokenService.class)).isTrue();
		assertThat(isAdvised(LuhnValidator.class)).isTrue();
	}

	@Test
	void infrastructureBeansAreNotAdvised() {
		assertThat(isAdvised(GlobalExceptionHandler.class)).isFalse();
		assertThat(isAdvised(OpenApiConfig.class)).isFalse();
	}

}
