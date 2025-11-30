package com.myou.ec.ecsite.domain.auth.policy;

import java.util.Arrays;
import java.util.Objects;

/**
 * Domain側のパスワード違反情報（application/presentationに依存しない）
 * messageKey は業務Tが messages.properties 等で文言管理できることを想定。
 */
public record PasswordViolation(
        String messageKey,
        Object[] args
) {
    public PasswordViolation {
        Objects.requireNonNull(messageKey);
        args = (args == null) ? new Object[0] : Arrays.copyOf(args, args.length);
    }

    public static PasswordViolation of(String messageKey, Object... args) {
        return new PasswordViolation(messageKey, args);
    }
}
