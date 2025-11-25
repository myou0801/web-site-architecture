package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;

import java.time.LocalDateTime;

public record AuthPasswordHistoryRecord(
        Long authPasswordHistoryId,
        long authUserId,
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
                new AuthUserId(authUserId),
                new EncodedPassword(loginPassword),
                PasswordChangeType.valueOf(changeType),
                changedAt,
                new LoginId(changedBy),
                createdAt,
                new LoginId(createdBy)
        );
    }

    public static AuthPasswordHistoryRecord fromDomain(PasswordHistory history) {
        return new AuthPasswordHistoryRecord(
                history.id(),
                history.authUserId().value(),
                history.encodedPassword().value(),
                history.changeType().name(),
                history.changedAt(),
                history.changedBy().value(),
                history.createdAt(),
                history.createdBy().value()
        );
    }
}
