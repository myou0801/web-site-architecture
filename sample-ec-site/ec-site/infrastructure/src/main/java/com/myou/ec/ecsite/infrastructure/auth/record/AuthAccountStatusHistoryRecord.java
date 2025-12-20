package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthAccountStatusHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AccountStatus;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountStatusHistoryId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record AuthAccountStatusHistoryRecord(
        @Nullable Long authAccountStatusHistoryId,
        Long authAccountId,
        @Nullable String fromStatus,
        String toStatus,
        String reason,
        LocalDateTime occurredAt,
        @Nullable String operatedBy,
        @Nullable LocalDateTime createdAt, // Nullable as per policy
        String createdBy
) {
    public static AuthAccountStatusHistoryRecord fromDomain(AuthAccountStatusHistory history, Operator operator) {
        return new AuthAccountStatusHistoryRecord(
                history.getId() != null ? history.getId().value() : null,
                history.getAuthAccountId().value(),
                history.getFromStatus() != null ? history.getFromStatus().name() : null,
                history.getToStatus().name(),
                history.getReason(),
                history.getOccurredAt(),
                history.getOperatedBy() != null ? history.getOperatedBy().value() : null,
                null, // createdAt is handled by DB
                operator.value()
        );
    }

    public AuthAccountStatusHistory toDomain() {
        return new AuthAccountStatusHistory(
                authAccountStatusHistoryId != null ? new AuthAccountStatusHistoryId(authAccountStatusHistoryId) : null,
                new AuthAccountId(authAccountId),
                fromStatus != null ? AccountStatus.valueOf(fromStatus) : null,
                AccountStatus.valueOf(toStatus),
                reason,
                occurredAt,
                operatedBy != null ? Operator.of(operatedBy) : null
        );
    }
}
