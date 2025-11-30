package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;

public record AuthPasswordHistoryRecord(
        Long authPasswordHistoryId,
        long authAccountId,
        String passwordHash,
        String changeType,
        LocalDateTime changedAt,
        String operatedBy,
        LocalDateTime createdAt,
        String createdBy
) {

    public PasswordHistory toDomain() {
        return new PasswordHistory(
                authPasswordHistoryId,
                new AuthAccountId(authAccountId),
                new PasswordHash(passwordHash),
                PasswordChangeType.valueOf(changeType),
                changedAt,
                new UserId(operatedBy),
                createdAt,
                new UserId(createdBy)
        );
    }

    public static AuthPasswordHistoryRecord fromDomain(PasswordHistory history) {
        return new AuthPasswordHistoryRecord(
                history.id(),
                history.authAccountId().value(),
                history.passwordHash().value(),
                history.changeType().name(),
                history.changedAt(),
                history.operatedBy().value(),
                history.createdAt(),
                history.createdBy().value()
        );
    }
}
