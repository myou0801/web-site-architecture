package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.value.AccountStatus;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountStatusHistoryId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * アカウント状態変更履歴エンティティ。
 */
public class AuthAccountStatusHistory {

    private final AuthAccountStatusHistoryId id;
    private final AuthAccountId authAccountId;
    private final AccountStatus fromStatus;
    private final AccountStatus toStatus;
    private final String reason;
    private final LocalDateTime occurredAt;
    private final UserId operatedBy;
    private final LocalDateTime createdAt;
    private final UserId createdBy;

    public AuthAccountStatusHistory(
            AuthAccountStatusHistoryId id,
            AuthAccountId authAccountId,
            AccountStatus fromStatus,
            AccountStatus toStatus,
            String reason,
            LocalDateTime occurredAt,
            UserId operatedBy,
            LocalDateTime createdAt,
            UserId createdBy
    ) {
        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId);
        this.toStatus = Objects.requireNonNull(toStatus);
        this.fromStatus = fromStatus;
        this.reason = Objects.requireNonNull(reason);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.operatedBy = operatedBy;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.createdBy = Objects.requireNonNull(createdBy);

        if (fromStatus == AccountStatus.DELETED) {
            throw new AuthDomainException("Cannot transition from DELETED status.");
        }
    }

    public static AuthAccountStatusHistory forNewAccount(
            AuthAccountId authAccountId,
            LocalDateTime occurredAt,
            UserId operator,
            String reason
    ) {
        return new AuthAccountStatusHistory(
                null,
                authAccountId,
                AccountStatus.ACTIVE,
                AccountStatus.ACTIVE,
                reason,
                occurredAt,
                operator,
                occurredAt,
                operator
        );
    }

    public static AuthAccountStatusHistory forStatusChange(
            AuthAccountId authAccountId,
            AccountStatus fromStatus,
            AccountStatus toStatus,
            LocalDateTime occurredAt,
            UserId operator,
            String reason
    ) {
        return new AuthAccountStatusHistory(
                null,
                authAccountId,
                fromStatus,
                toStatus,
                reason,
                occurredAt,
                operator,
                occurredAt,
                operator
        );
    }

    // Getters
    public AuthAccountStatusHistoryId getId() {
        return id;
    }

    public AuthAccountId getAuthAccountId() {
        return authAccountId;
    }



    public AccountStatus getFromStatus() {
        return fromStatus;
    }

    public AccountStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public UserId getOperatedBy() {
        return operatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserId getCreatedBy() {
        return createdBy;
    }
}
