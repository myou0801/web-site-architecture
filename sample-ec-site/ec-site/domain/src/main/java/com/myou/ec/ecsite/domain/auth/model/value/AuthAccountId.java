package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * 認証アカウントIDを表す値オブジェクト。
 */
public record AuthAccountId(long value) {

    public AuthAccountId {
        if (value <= 0) {
            throw new IllegalArgumentException("authAccountId must be positive.");
        }
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
