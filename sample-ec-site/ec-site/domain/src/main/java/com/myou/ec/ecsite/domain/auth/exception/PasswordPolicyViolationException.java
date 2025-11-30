package com.myou.ec.ecsite.domain.auth.exception;

import com.myou.ec.ecsite.domain.auth.policy.PasswordViolation;

import java.util.ArrayList;
import java.util.List;

/**
 * パスワードポリシー違反（構文NGなど）の例外。
 */
public class PasswordPolicyViolationException extends AuthDomainException {

    private final List<PasswordViolation> passwordViolations;

    public PasswordPolicyViolationException(PasswordViolation passwordViolation) {
        super("パスワードエラー");
        this.passwordViolations = new ArrayList<>();
        this.passwordViolations.add(passwordViolation);
    }

    public PasswordPolicyViolationException(List<PasswordViolation> passwordViolations) {
        super("パスワードエラー");
        this.passwordViolations = passwordViolations;
    }
}
