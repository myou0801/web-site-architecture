package com.myou.ec.ecsite.domain.auth.model.policy.rules;

import com.myou.ec.ecsite.domain.auth.model.policy.PasswordRule;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordViolation;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.util.Optional;

public class NotSameAsLoginIdRule implements PasswordRule {

    private final String messageKey;

    public NotSameAsLoginIdRule(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public Optional<PasswordViolation> validate(String newRawPassword, LoginId loginId) {
        if (newRawPassword == null || loginId == null) return Optional.empty();
        String t = newRawPassword.trim();
        if (t.equals(loginId.value())) {
            return Optional.of(PasswordViolation.of(messageKey));
        }
        return Optional.empty();
    }
}
