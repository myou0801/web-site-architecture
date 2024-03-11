package com.myou.ec.ecsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class EcSiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcSiteApplication.class, args);
	}



	@EnableSpringHttpSession
	@Configuration
	@Conditional(ProfileCondition.class)
	static class SpringHttpSessionConfig {
		@Bean
		MapSessionRepository sessionRepository() {
			return new MapSessionRepository(new ConcurrentHashMap<>());
		}
	}

	static class ProfileCondition implements Condition {
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !context.getEnvironment().matchesProfiles("docker");
		}
	}
}
