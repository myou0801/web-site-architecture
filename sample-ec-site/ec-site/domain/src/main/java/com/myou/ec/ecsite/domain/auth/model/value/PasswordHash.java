package com.myou.ec.ecsite.domain.auth.model.value;

/**
 * ハッシュ済みパスワードを表す値オブジェクト。
 * 生パスワードはここでは扱わない。
 */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("encodedPassword must not be blank.");
        }
    }

    @Override
    public String toString() {
        // toStringで中身をそのまま出さないようにしておく。
        return "*****";
    }
}
