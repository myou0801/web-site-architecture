package com.myou.ec.ecsite.domain.auth.exception;

/**
 * パスワードポリシー違反（構文NGなど）の例外。
 */
public class PasswordPolicyViolationException extends AuthDomainException {

    public PasswordPolicyViolationException(String message) {
        super(message);
    }
}
