package com.myou.ec.ecsite.domain.auth.exception;

public class AuthRoleDisabledException extends RuntimeException {
    public AuthRoleDisabledException(String message) {
        super(message);
    }
}
