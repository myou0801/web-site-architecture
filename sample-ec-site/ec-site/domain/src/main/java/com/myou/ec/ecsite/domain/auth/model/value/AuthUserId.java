package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * 認証ユーザIDを表す値オブジェクト。
 */
public record AuthUserId(long value) {

    public AuthUserId {
        if (value <= 0) {
            throw new IllegalArgumentException("authUserId must be positive.");
        }
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
