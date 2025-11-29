package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * ユーザーIDを表す値オブジェクト。
 */
public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
