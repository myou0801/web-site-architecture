package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * アカウントロック／ロック解除のイベント履歴 Entity。
 *
 * @param locked true=ロック / false=ロック解除
 * @param reason 例: LOGIN_FAIL_THRESHOLD, ADMIN_UNLOCK 等
 */
public record AccountLockEvent(Long id, AuthAccountId authAccountId, boolean locked, String reason,
                               LocalDateTime occurredAt,
                               UserId operatedBy, LocalDateTime createdAt, UserId createdBy) {

    public AccountLockEvent(Long id,
                            AuthAccountId authAccountId,
                            boolean locked,
                            String reason,
                            LocalDateTime occurredAt,
                            UserId operatedBy,
                            LocalDateTime createdAt,
                            UserId createdBy) {

        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId, "authAccountId must not be null");
        this.locked = locked;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.reason = reason;
        this.operatedBy = Objects.requireNonNull(operatedBy, "operatedBy must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static AccountLockEvent lock(AuthAccountId authAccountId,
                                        LocalDateTime now,
                                        String reason,
                                        UserId operatedBy) {
        return new AccountLockEvent(null, authAccountId, true, reason, now, operatedBy, now, operatedBy);
    }

    public static AccountLockEvent unlock(AuthAccountId authAccountId,
                                          LocalDateTime now,
                                          String reason,
                                          UserId operatedBy) {
        return new AccountLockEvent(null, authAccountId, false, reason, now, operatedBy, now, operatedBy);
    }
}
