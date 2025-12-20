package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.model.value.AccountStatus;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountStatusHistoryId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;

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
    private final Operator operatedBy;

    public AuthAccountStatusHistory(
            AuthAccountStatusHistoryId id,
            AuthAccountId authAccountId,
            AccountStatus fromStatus,
            AccountStatus toStatus,
            String reason,
            LocalDateTime occurredAt,
            Operator operatedBy
    ) {
        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId);
        this.toStatus = Objects.requireNonNull(toStatus);
        this.fromStatus = fromStatus;
        this.reason = Objects.requireNonNull(reason);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.operatedBy = operatedBy;

        if (fromStatus == AccountStatus.DELETED) {
            throw new AuthDomainException("Cannot transition from DELETED status.");
        }
    }

    public static AuthAccountStatusHistory forNewAccount(
            AuthAccountId authAccountId,
            LocalDateTime occurredAt,
            Operator operator,
            String reason
    ) {
        return new AuthAccountStatusHistory(
                null,
                authAccountId,
                AccountStatus.ACTIVE,
                AccountStatus.ACTIVE,
                reason,
                occurredAt,
                operator
        );
    }

    public static AuthAccountStatusHistory forActivating(
            AuthAccountId authAccountId,
            AccountStatus fromStatus,
            LocalDateTime occurredAt,
            Operator operator,
            String reason
    ) {
        AccountStatus toStatus = AccountStatus.ACTIVE;
        if (fromStatus == toStatus) {
            throw new AuthDomainException("Cannot transition to the same status.");
        }
        return new AuthAccountStatusHistory(
                null,
                authAccountId,
                fromStatus,
                toStatus,
                reason,
                occurredAt,
                operator
        );
    }

    public static AuthAccountStatusHistory forDisabling(
            AuthAccountId authAccountId,
            AccountStatus fromStatus,
            LocalDateTime occurredAt,
            Operator operator,
            String reason
    ) {
        AccountStatus toStatus = AccountStatus.DISABLED;
        if (fromStatus == toStatus) {
            throw new AuthDomainException("Cannot transition to the same status.");
        }
        return new AuthAccountStatusHistory(
                null,
                authAccountId,
                fromStatus,
                toStatus,
                reason,
                occurredAt,
                operator
        );
    }

    public static AuthAccountStatusHistory forDeleting(
            AuthAccountId authAccountId,
            AccountStatus fromStatus,
            LocalDateTime occurredAt,
            Operator operator,
            String reason
    ) {
        AccountStatus toStatus = AccountStatus.DELETED;
        if (fromStatus == toStatus) {
            throw new AuthDomainException("Cannot transition to the same status.");
        }
        return new AuthAccountStatusHistory(
                null,
                authAccountId,
                fromStatus,
                toStatus,
                reason,
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

    public Operator getOperatedBy() {
        return operatedBy;
    }
}
