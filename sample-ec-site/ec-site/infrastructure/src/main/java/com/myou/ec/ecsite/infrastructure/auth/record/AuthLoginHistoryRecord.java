package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;

public record AuthLoginHistoryRecord(
        Long authLoginHistoryId,
        long authAccountId,
        String result,
        LocalDateTime loginAt,
        LocalDateTime createdAt,
        String createdBy
) {

    public LoginHistory toDomain() {
        return new LoginHistory(
                authLoginHistoryId,
                new AuthAccountId(authAccountId),
                LoginResult.valueOf(result),
                loginAt,
                createdAt,
                new UserId(createdBy)
        );
    }

    public static AuthLoginHistoryRecord fromDomain(LoginHistory history) {
        return new AuthLoginHistoryRecord(
                history.id(),
                history.authAccountId().value(),
                history.result().name(),
                history.loginAt(),
                history.createdAt(),
                history.createdBy().value()
        );
    }
}
