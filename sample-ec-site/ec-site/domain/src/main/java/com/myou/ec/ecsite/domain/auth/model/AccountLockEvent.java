package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * アカウントロック／ロック解除のイベント履歴 Entity。
 */
public class AccountLockEvent {

    private final Long id;
    private final AuthAccountId authAccountId;
    private final boolean locked;          // true=ロック / false=ロック解除
    private final LocalDateTime occurredAt;
    private final String reason;           // 例: LOGIN_FAIL_THRESHOLD, ADMIN_UNLOCK 等
    private final UserId operatedBy;
    private final LocalDateTime createdAt;
    private final UserId createdBy;

    public AccountLockEvent(Long id,
                            AuthAccountId authAccountId,
                            boolean locked,
                            LocalDateTime occurredAt,
                            String reason,
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
        return new AccountLockEvent(null, authAccountId, true, now, reason, operatedBy, now, operatedBy);
    }

    public static AccountLockEvent unlock(AuthAccountId authAccountId,
                                          LocalDateTime now,
                                          String reason,
                                          UserId operatedBy) {
        return new AccountLockEvent(null, authAccountId, false, now, reason, operatedBy, now, operatedBy);
    }

    public Long id() {
        return id;
    }

    public AuthAccountId authAccountId() {
        return authAccountId;
    }

    public boolean locked() {
        return locked;
    }

    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    public String reason() {
        return reason;
    }

    public UserId operatedBy() {
        return operatedBy;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdBy() {
        return createdBy;
    }
}
