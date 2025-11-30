package com.myou.ec.ecsite.domain.auth.policy.rules;

import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.policy.PasswordRule;
import com.myou.ec.ecsite.domain.auth.policy.PasswordViolation;

import java.util.Optional;
import java.util.regex.Pattern;

public class AlphaNumericRule implements PasswordRule {

    private static final Pattern ALNUM = Pattern.compile("^[0-9A-Za-z]+$");
    private final String messageKey;

    public AlphaNumericRule(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public Optional<PasswordViolation> validate(String newRawPassword, UserId userId) {
        if (newRawPassword == null) return Optional.empty();
        String t = newRawPassword.trim();
        if (!ALNUM.matcher(t).matches()) {
            return Optional.of(PasswordViolation.of(messageKey));
        }
        return Optional.empty();
    }
}
