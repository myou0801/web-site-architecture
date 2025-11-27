package com.myou.ec.ecsite.domain.auth.model.policy;

import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.util.Optional;

public interface PasswordRule {
    Optional<PasswordViolation> validate(String newRawPassword, LoginId loginId);
}
