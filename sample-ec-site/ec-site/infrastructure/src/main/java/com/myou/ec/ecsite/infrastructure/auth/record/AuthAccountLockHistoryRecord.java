package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record AuthAccountLockHistoryRecord(
        Long authAccountLockHistoryId,
        long authAccountId,
        boolean locked,
        String reason,
        LocalDateTime occurredAt,
        @Nullable String operatedBy,
        @Nullable LocalDateTime createdAt, // Nullable as per policy
        String createdBy
) {

    public AccountLockEvent toDomain() {
        return new AccountLockEvent(
                authAccountLockHistoryId,
                new AuthAccountId(authAccountId),
                locked,
                reason,
                occurredAt,
                operatedBy != null ? Operator.of(operatedBy) : null
        );
    }

    public static AuthAccountLockHistoryRecord fromDomain(AccountLockEvent event, Operator operator) {
        return new AuthAccountLockHistoryRecord(
                event.id(),
                event.authAccountId().value(),
                event.locked(),
                event.reason(),
                event.occurredAt(),
                event.operatedBy() != null ? event.operatedBy().value() : null,
                null, // createdAt is handled by DB
                operator.value()
        );
    }
}
