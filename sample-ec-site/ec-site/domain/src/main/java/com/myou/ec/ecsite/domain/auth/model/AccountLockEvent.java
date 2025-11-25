package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * アカウントロック／ロック解除のイベント履歴 Entity。
 */
public class AccountLockEvent {

    private final Long id;
    private final AuthUserId authUserId;
    private final boolean locked;          // true=ロック / false=ロック解除
    private final LocalDateTime occurredAt;
    private final String reason;           // 例: LOGIN_FAIL_THRESHOLD, ADMIN_UNLOCK 等
    private final LoginId operatedBy;
    private final LocalDateTime createdAt;
    private final LoginId createdBy;

    public AccountLockEvent(Long id,
                            AuthUserId authUserId,
                            boolean locked,
                            LocalDateTime occurredAt,
                            String reason,
                            LoginId operatedBy,
                            LocalDateTime createdAt,
                            LoginId createdBy) {

        this.id = id;
        this.authUserId = Objects.requireNonNull(authUserId, "authUserId must not be null");
        this.locked = locked;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.reason = reason;
        this.operatedBy = Objects.requireNonNull(operatedBy, "operatedBy must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static AccountLockEvent lock(AuthUserId authUserId,
                                        LocalDateTime now,
                                        String reason,
                                        LoginId operatedBy) {
        return new AccountLockEvent(null, authUserId, true, now, reason, operatedBy, now, operatedBy);
    }

    public static AccountLockEvent unlock(AuthUserId authUserId,
                                          LocalDateTime now,
                                          String reason,
                                          LoginId operatedBy) {
        return new AccountLockEvent(null, authUserId, false, now, reason, operatedBy, now, operatedBy);
    }

    public Long id() {
        return id;
    }

    public AuthUserId authUserId() {
        return authUserId;
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

    public LoginId operatedBy() {
        return operatedBy;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LoginId createdBy() {
        return createdBy;
    }
}
