package com.myou.ec.ecsite.application.auth.config;

import com.myou.ec.ecsite.domain.auth.model.policy.BasicPasswordPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.FixedThresholdLockPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.LockPolicy;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthPolicyConfig {

    // These values would typically come from application properties
    // For now, hardcoding as per markdown's example
    @Bean
    public PasswordPolicy passwordPolicy() {
        return new BasicPasswordPolicy();
    }

    @Bean
    public LockPolicy lockPolicy() {
        return new FixedThresholdLockPolicy();
    }
}
