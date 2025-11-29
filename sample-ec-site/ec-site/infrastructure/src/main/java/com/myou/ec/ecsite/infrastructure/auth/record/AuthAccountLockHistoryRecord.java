package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;

public record AuthAccountLockHistoryRecord(
        Long authAccountLockHistoryId,
        long authAccountId,
        boolean locked,
        LocalDateTime occurredAt,
        String reason,
        String operatedBy,
        LocalDateTime createdAt,
        String createdBy
) {

    public AccountLockEvent toDomain() {
        return new AccountLockEvent(
                authAccountLockHistoryId,
                new AuthAccountId(authAccountId),
                locked,
                occurredAt,
                reason,
                new UserId(operatedBy),
                createdAt,
                new UserId(createdBy)
        );
    }

    public static AuthAccountLockHistoryRecord fromDomain(AccountLockEvent event) {
        return new AuthAccountLockHistoryRecord(
                event.id(),
                event.authAccountId().value(),
                event.locked(),
                event.occurredAt(),
                event.reason(),
                event.operatedBy().value(),
                event.createdAt(),
                event.createdBy().value()
        );
    }
}
