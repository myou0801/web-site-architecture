package com.myou.ec.ecsite.domain.auth.exception;

/**
 * 認証ドメイン共通の基底例外。
 */
public class AuthDomainException extends RuntimeException {

    public AuthDomainException(String message) {
        super(message);
    }

    public AuthDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
