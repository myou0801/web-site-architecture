package com.myou.ec.ecsite.domain.auth.model.policy.rules;

import com.myou.ec.ecsite.domain.auth.model.policy.PasswordRule;
import com.myou.ec.ecsite.domain.auth.model.policy.PasswordViolation;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.util.Optional;

public class MinLengthRule implements PasswordRule {

    private final int minLength;
    private final String messageKey;

    public MinLengthRule(int minLength, String messageKey) {
        this.minLength = minLength;
        this.messageKey = messageKey;
    }

    @Override
    public Optional<PasswordViolation> validate(String newRawPassword, LoginId loginId) {
        if (newRawPassword == null) return Optional.empty(); // RequiredRuleで拾う
        String t = newRawPassword.trim();
        if (t.length() < minLength) {
            return Optional.of(PasswordViolation.of(messageKey, minLength));
        }
        return Optional.empty();
    }
}