package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;
import com.myou.ec.ecsite.domain.auth.model.value.Operator;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record AuthLoginHistoryRecord(
        Long authLoginHistoryId,
        long authAccountId,
        String result,
        LocalDateTime loginAt,
        @Nullable LocalDateTime createdAt, // Nullable as per policy
        String createdBy
) {

    public LoginHistory toDomain() {
        return new LoginHistory(
                authLoginHistoryId,
                new AuthAccountId(authAccountId),
                LoginResult.valueOf(result),
                loginAt
        );
    }

    public static AuthLoginHistoryRecord fromDomain(LoginHistory history, Operator operator) {
        return new AuthLoginHistoryRecord(
                history.id(),
                history.authAccountId().value(),
                history.result().name(),
                history.loginAt(),
                null, // createdAt is handled by DB
                operator.value()
        );
    }
}
