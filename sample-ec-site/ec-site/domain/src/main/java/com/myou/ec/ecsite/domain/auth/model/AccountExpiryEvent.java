package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AccountExpiryEventType;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

public record AccountExpiryEvent(
        AuthAccountId accountId,
        AccountExpiryEventType eventType,
        String reason,
        LocalDateTime occurredAt,
        UserId operatedBy // nullable for EXPIRE
) {
    public AccountExpiryEvent {
        requireNonNull(accountId, "accountId");
        requireNonNull(eventType, "eventType");
        requireNonNull(reason, "reason");
        requireNonNull(occurredAt, "occurredAt");
    }

    public static AccountExpiryEvent expired(AuthAccountId accountId,
                                             String reason,
                                             LocalDateTime occurredAt,
                                             UserId operatedBy ) {
        return  new AccountExpiryEvent(
                accountId,
                AccountExpiryEventType.EXPIRE,
                reason,
                occurredAt,
                operatedBy
        );
    }

    public static AccountExpiryEvent unexpired(AuthAccountId accountId,
                                             String reason,
                                             LocalDateTime occurredAt,
                                             UserId operatedBy ) {
        return  new AccountExpiryEvent(
                accountId,
                AccountExpiryEventType.UNEXPIRE,
                reason,
                occurredAt,
                operatedBy
        );
    }

    public boolean isExpire() { return eventType == AccountExpiryEventType.EXPIRE; }
    public boolean isUnexpire() { return eventType == AccountExpiryEventType.UNEXPIRE; }
}
