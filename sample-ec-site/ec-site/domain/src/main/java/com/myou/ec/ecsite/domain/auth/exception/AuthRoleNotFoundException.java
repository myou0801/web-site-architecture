package com.myou.ec.ecsite.domain.auth.exception;

public class AuthRoleNotFoundException extends RuntimeException {
    public AuthRoleNotFoundException(String message) {
        super(message);
    }
}
