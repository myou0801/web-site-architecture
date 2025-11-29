package com.myou.ec.ecsite.domain.auth.model.policy.rules;

import com.myou.ec.ecsite.domain.auth.model.policy.PasswordRule;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordViolation;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.util.Optional;

public class NotSameAsLoginIdRule implements PasswordRule {

    private final String messageKey;

    public NotSameAsLoginIdRule(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public Optional<PasswordViolation> validate(String newRawPassword, UserId userId) {
        if (newRawPassword == null || userId == null) return Optional.empty();
        String t = newRawPassword.trim();
        if (t.equals(userId.value())) {
            return Optional.of(PasswordViolation.of(messageKey));
        }
        return Optional.empty();
    }
}
