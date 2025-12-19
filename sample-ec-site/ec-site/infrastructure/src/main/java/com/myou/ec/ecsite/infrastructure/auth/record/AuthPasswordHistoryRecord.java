package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record AuthPasswordHistoryRecord(
        Long authPasswordHistoryId,
        long authAccountId,
        String passwordHash,
        String changeType,
        LocalDateTime changedAt,
        @Nullable String operatedBy,
        @Nullable LocalDateTime createdAt, // Nullable as per policy
        String createdBy
) {

    public PasswordHistory toDomain() {
        return new PasswordHistory(
                authPasswordHistoryId,
                new AuthAccountId(authAccountId),
                new PasswordHash(passwordHash),
                PasswordChangeType.valueOf(changeType),
                changedAt,
                operatedBy != null ? new UserId(operatedBy) : null
        );
    }

    public static AuthPasswordHistoryRecord fromDomain(PasswordHistory history, UserId createdByApp) {
        return new AuthPasswordHistoryRecord(
                history.id(),
                history.authAccountId().value(),
                history.passwordHash().value(),
                history.changeType().name(),
                history.changedAt(),
                history.operatedBy() != null ? history.operatedBy().value() : null,
                null, // createdAt is handled by DB
                createdByApp.value()
        );
    }
}
