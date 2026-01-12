package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AccountStatus;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

/**
 * AUTH_ACCOUNT テーブルの1行を表す Record。
 */
public record AuthAccountRecord(
        @Nullable Long authAccountId,
        String loginId,
        String passwordHash,
        String accountStatus, // ACTIVE, DISABLED, DELETED
        @Nullable LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {

    public AuthAccount toDomain() {
        return new AuthAccount(
                authAccountId != null ? new AuthAccountId(authAccountId) : null,
                new LoginId(loginId),
                new PasswordHash(passwordHash),
                AccountStatus.valueOf(accountStatus)
        );
    }
}
