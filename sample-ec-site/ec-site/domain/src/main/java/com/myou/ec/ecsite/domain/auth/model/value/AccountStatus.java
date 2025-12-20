package com.myou.ec.ecsite.domain.auth.model.value;

import java.util.EnumSet;
import java.util.Objects;

public enum AccountStatus {
    ACTIVE,
    DISABLED,
    DELETED;

    /**
     * 指定された toStatus への遷移が許可されているかを返す。
     */
    public boolean canTransitionTo(AccountStatus toStatus) {
        Objects.requireNonNull(toStatus, "toStatus");
        // no-op は「遷移」としては扱わない（履歴INSERTもしない方針）
        if (this == toStatus) {
            return false;
        }

        return switch (this) {
            case ACTIVE -> EnumSet.of(DISABLED, DELETED).contains(toStatus);
            case DISABLED -> EnumSet.of(ACTIVE, DELETED).contains(toStatus);
            case DELETED -> false; // 終端
        };
    }
}
