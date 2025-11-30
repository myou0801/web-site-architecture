package com.myou.ec.ecsite.domain.auth.policy;

import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.util.Optional;

public interface PasswordRule {
    Optional<PasswordViolation> validate(String newRawPassword, UserId userId);
}
