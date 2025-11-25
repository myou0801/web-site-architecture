package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * ログインID（ユーザID）を表す値オブジェクト。
 */
public record LoginId(String value) {

    public LoginId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("loginId must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
