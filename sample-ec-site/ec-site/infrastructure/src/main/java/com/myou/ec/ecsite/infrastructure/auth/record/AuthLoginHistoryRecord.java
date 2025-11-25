package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;

public record AuthLoginHistoryRecord(
        Long authLoginHistoryId,
        long authUserId,
        LocalDateTime loginAt,
        String result,
        LocalDateTime createdAt,
        String createdBy
) {

    public LoginHistory toDomain() {
        return new LoginHistory(
                authLoginHistoryId,
                new AuthUserId(authUserId),
                loginAt,
                LoginResult.valueOf(result),
                createdAt,
                new LoginId(createdBy)
        );
    }

    public static AuthLoginHistoryRecord fromDomain(LoginHistory history) {
        return new AuthLoginHistoryRecord(
                history.id(),
                history.authUserId().value(),
                history.loginAt(),
                history.result().name(),
                history.createdAt(),
                history.createdBy().value()
        );
    }
}
