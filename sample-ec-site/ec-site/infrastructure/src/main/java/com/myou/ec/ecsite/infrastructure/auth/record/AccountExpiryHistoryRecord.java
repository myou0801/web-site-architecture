package com.myou.ec.ecsite.infrastructure.auth.record;


import com.myou.ec.ecsite.domain.auth.model.AccountExpiryEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AccountExpiryEventType;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record AccountExpiryHistoryRecord(
        Long authAccountExpiryHistoryId,
        long authAccountId,
        String eventType,
        String reason,
        LocalDateTime occurredAt,
        @Nullable String operatedBy, // Nullable as per DDL
        @Nullable LocalDateTime createdAt, // Nullable as per policy
        String createdBy
) {
    public AccountExpiryEvent toDomain() {
        return new AccountExpiryEvent(
                new AuthAccountId(authAccountId),
                AccountExpiryEventType.valueOf(eventType),
                reason,
                occurredAt,
                operatedBy != null ? new UserId(operatedBy) : null
        );
    }

    public static AccountExpiryHistoryRecord fromDomain(AccountExpiryEvent ev, UserId createdBy) {
        return new AccountExpiryHistoryRecord(
                null,
                ev.accountId().value(),
                ev.eventType().name(),
                ev.reason(),
                ev.occurredAt(),
                ev.operatedBy() != null ? ev.operatedBy().value() : null,
                null, // createdAt is handled by DB
                createdBy.value()
        );
    }
}
