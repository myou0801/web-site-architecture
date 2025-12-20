package com.myou.ec.ecsite.config;

import com.myou.ec.ecsite.domain.auth.policy.*;
import com.myou.ec.ecsite.domain.auth.policy.rules.AlphaNumericRule;
import com.myou.ec.ecsite.domain.auth.policy.rules.MinLengthRule;
import com.myou.ec.ecsite.domain.auth.policy.rules.NotSameAsLoginIdRule;
import com.myou.ec.ecsite.domain.auth.policy.rules.RequiredRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.List;

@Configuration
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public PasswordPolicy passwordPolicy() {
        return new CompositePasswordPolicy(List.of(
                new RequiredRule("auth.password.new.required"),
                new MinLengthRule(5, "auth.password.new.minLength"),
                new AlphaNumericRule("auth.password.new.alphanumeric"),
                new NotSameAsLoginIdRule("auth.password.new.sameAsLoginId")
        ));
    }

    @Bean
    public LockPolicy lockPolicy() {
        return new FixedThresholdLockPolicy();
    }


    @Bean
    public AccountExpiryPolicy  accountExpiryPolicy() {
        return new AccountExpiryPolicy(90);
    }

}
