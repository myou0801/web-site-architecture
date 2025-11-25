package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.time.LocalDateTime;

public record AuthAccountLockHistoryRecord(
        Long authAccountLockHistoryId,
        long authUserId,
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
                new AuthUserId(authUserId),
                locked,
                occurredAt,
                reason,
                new LoginId(operatedBy),
                createdAt,
                new LoginId(createdBy)
        );
    }

    public static AuthAccountLockHistoryRecord fromDomain(AccountLockEvent event) {
        return new AuthAccountLockHistoryRecord(
                event.id(),
                event.authUserId().value(),
                event.locked(),
                event.occurredAt(),
                event.reason(),
                event.operatedBy().value(),
                event.createdAt(),
                event.createdBy().value()
        );
    }
}
