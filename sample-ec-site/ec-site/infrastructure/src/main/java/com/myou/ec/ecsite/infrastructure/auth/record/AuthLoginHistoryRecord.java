package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;

public record AuthLoginHistoryRecord(
        Long authLoginHistoryId,
        long authAccountId,
        LocalDateTime loginAt,
        String result,
        LocalDateTime createdAt,
        String createdBy
) {

    public LoginHistory toDomain() {
        return new LoginHistory(
                authLoginHistoryId,
                new AuthAccountId(authAccountId),
                loginAt,
                LoginResult.valueOf(result),
                createdAt,
                new UserId(createdBy)
        );
    }

    public static AuthLoginHistoryRecord fromDomain(LoginHistory history) {
        return new AuthLoginHistoryRecord(
                history.id(),
                history.authAccountId().value(),
                history.loginAt(),
                history.result().name(),
                history.createdAt(),
                history.createdBy().value()
        );
    }
}
