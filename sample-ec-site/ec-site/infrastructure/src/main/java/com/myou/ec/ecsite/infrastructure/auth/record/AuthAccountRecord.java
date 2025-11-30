package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * AUTH_ACCOUNT テーブルの1行を表す Record。
 */
public record AuthAccountRecord(
        @Nullable Long authAccountId,
        String userId,
        String passwordHash,
        boolean enabled,
        boolean deleted,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        @Nullable LocalDateTime deletedAt,
        @Nullable String deletedBy

) {

    public AuthAccount toDomain() {
        return new AuthAccount(
                authAccountId != null ? new AuthAccountId(authAccountId) : null,
                new UserId(userId),
                new PasswordHash(passwordHash),
                enabled,
                deleted,
                createdAt,
                new UserId(createdBy),
                updatedAt,
                new UserId(updatedBy),
                deletedAt,
                deletedBy != null ? new UserId(deletedBy) : null
        );
    }

    public static AuthAccountRecord fromDomain(AuthAccount user) {
        Long id = user.id() != null ? user.id().value() : null;
        return new AuthAccountRecord(
                id,
                user.userId().value(),
                user.passwordHash().value(),
                user.enabled(),
                user.deleted(),
                user.createdAt(),
                user.createdBy().value(),
                user.updatedAt(),
                user.updatedBy().value(),
                user.deletedAt(),
                Optional.ofNullable(user.deletedBy()).map(UserId::value).orElse(null)
        );
    }
}
