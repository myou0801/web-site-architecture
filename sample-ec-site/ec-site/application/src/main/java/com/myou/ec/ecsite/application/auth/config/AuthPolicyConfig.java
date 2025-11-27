package com.myou.ec.ecsite.application.auth.config;

import com.myou.ec.ecsite.domain.auth.model.policy.CompositePasswordPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.FixedThresholdLockPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.LockPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.rules.AlphaNumericRule;
import com.myou.ec.ecsite.domain.auth.model.policy.rules.MinLengthRule;
import com.myou.ec.ecsite.domain.auth.model.policy.rules.NotSameAsLoginIdRule;
import com.myou.ec.ecsite.domain.auth.model.policy.rules.RequiredRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AuthPolicyConfig {

    // These values would typically come from application properties
    // For now, hardcoding as per markdown's example
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
}
