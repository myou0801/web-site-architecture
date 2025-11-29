package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;

import java.time.LocalDateTime;

public record AuthPasswordHistoryRecord(
        Long authPasswordHistoryId,
        long authAccountId,
        String loginPassword,
        String changeType,
        LocalDateTime changedAt,
        String changedBy,
        LocalDateTime createdAt,
        String createdBy
) {

    public PasswordHistory toDomain() {
        return new PasswordHistory(
                authPasswordHistoryId,
                new AuthAccountId(authAccountId),
                new EncodedPassword(loginPassword),
                PasswordChangeType.valueOf(changeType),
                changedAt,
                new UserId(changedBy),
                createdAt,
                new UserId(createdBy)
        );
    }

    public static AuthPasswordHistoryRecord fromDomain(PasswordHistory history) {
        return new AuthPasswordHistoryRecord(
                history.id(),
                history.authAccountId().value(),
                history.encodedPassword().value(),
                history.changeType().name(),
                history.changedAt(),
                history.changedBy().value(),
                history.createdAt(),
                history.createdBy().value()
        );
    }
}
