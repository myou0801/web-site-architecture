package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AccountStatus;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

/**
 * AUTH_ACCOUNT テーブルの1行を表す Record。
 */
public record AuthAccountRecord(
        @Nullable Long authAccountId,
        String userId,
        String passwordHash,
        String accountStatus, // ACTIVE, DISABLED, DELETED
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {

    public AuthAccount toDomain() {
        return new AuthAccount(
                authAccountId != null ? new AuthAccountId(authAccountId) : null,
                new UserId(userId),
                new PasswordHash(passwordHash),
                AccountStatus.valueOf(accountStatus),
                createdAt,
                new UserId(createdBy),
                updatedAt,
                new UserId(updatedBy)
        );
    }

    public static AuthAccountRecord fromDomain(AuthAccount user) {
        Long id = user.id() != null ? user.id().value() : null;
        return new AuthAccountRecord(
                id,
                user.userId().value(),
                user.passwordHash().value(),
                user.accountStatus().name(),
                user.createdAt(),
                user.createdBy().value(),
                user.updatedAt(),
                user.updatedBy().value()
        );
    }
}
