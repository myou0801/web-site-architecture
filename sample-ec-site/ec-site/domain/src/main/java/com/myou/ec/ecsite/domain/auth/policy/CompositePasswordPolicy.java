package com.myou.ec.ecsite.domain.auth.policy;

import com.myou.ec.ecsite.domain.auth.exception.PasswordPolicyViolationException;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompositePasswordPolicy implements PasswordPolicy {

    private final List<PasswordRule> rules;

    public CompositePasswordPolicy(List<PasswordRule> rules) {
        Objects.requireNonNull(rules);
        this.rules = List.copyOf(rules);
    }

    @Override
    public void validatePassword(String newRawPassword, UserId userId) throws PasswordPolicyViolationException {
        var violations = new ArrayList<PasswordViolation>();
        for (PasswordRule rule : rules) {
            rule.validate(newRawPassword, userId).ifPresent(violations::add);
        }
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }
    }
}
