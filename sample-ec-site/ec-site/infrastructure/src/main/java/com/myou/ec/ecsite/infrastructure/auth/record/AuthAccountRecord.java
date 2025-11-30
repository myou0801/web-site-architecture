package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AUTH_ACCOUNT テーブルの1行を表す Record。
 */
public record AuthAccountRecord(
        Long authAccountId,
        String userId,
        String passwordHash,
        boolean enabled,
        boolean deleted,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        LocalDateTime deletedAt,
        String deletedBy

) {

    public AuthAccount toDomain(List<RoleCode> roleCodes) {
        return new AuthAccount(
                authAccountId != null ? new AuthAccountId(authAccountId) : null,
                new UserId(userId),
                new PasswordHash(passwordHash),
                enabled,
                deleted,
                roleCodes,
                createdAt,
                new UserId(createdBy),
                updatedAt,
                new UserId(updatedBy),
                deletedAt,
                new UserId(deletedBy)
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
                user.deletedBy().value()
        );
    }
}
