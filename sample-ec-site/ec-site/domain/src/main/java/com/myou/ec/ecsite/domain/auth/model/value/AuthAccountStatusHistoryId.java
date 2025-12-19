package com.myou.ec.ecsite.domain.auth.model.value;

import java.io.Serializable;

/**
 * 認証アカウント状態履歴ID。
 *
 * @param value ID
 */
public record AuthAccountStatusHistoryId(Long value) implements Serializable {
    public AuthAccountStatusHistoryId {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null.");
        }
    }
}
