package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * 権限ロールコードを表す値オブジェクト。
 * 例: ROLE_ADMIN, ROLE_USER 等。
 */
public record RoleCode(String value) {

    public RoleCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("roleCode must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
