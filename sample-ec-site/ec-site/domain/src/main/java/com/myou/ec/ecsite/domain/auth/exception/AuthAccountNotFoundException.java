package com.myou.ec.ecsite.domain.auth.exception;

public class AuthAccountNotFoundException extends RuntimeException {
    public AuthAccountNotFoundException(String message) {
        super(message);
    }
}
