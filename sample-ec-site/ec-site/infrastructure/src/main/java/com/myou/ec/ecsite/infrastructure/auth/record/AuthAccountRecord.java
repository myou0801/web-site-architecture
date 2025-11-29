package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AUTH_ACCOUNT テーブルの1行を表す Record。
 */
public record AuthAccountRecord(
        Long authAccountId,
        String userId,
        String loginPassword,
        boolean enabled,
        boolean deleted,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        long versionNo
) {

    public AuthAccount toDomain(List<RoleCode> roleCodes) {
        return new AuthAccount(
                authAccountId != null ? new AuthAccountId(authAccountId) : null,
                new UserId(userId),
                new EncodedPassword(loginPassword),
                enabled,
                deleted,
                roleCodes,
                createdAt,
                new UserId(createdBy),
                updatedAt,
                new UserId(updatedBy),
                versionNo
        );
    }

    public static AuthAccountRecord fromDomain(AuthAccount user) {
        Long id = user.id() != null ? user.id().value() : null;
        return new AuthAccountRecord(
                id,
                user.userId().value(),
                user.encodedPassword().value(),
                user.enabled(),
                user.deleted(),
                user.createdAt(),
                user.createdByUserId().value(),
                user.updatedAt(),
                user.updatedByUserId().value(),
                user.versionNo()
        );
    }
}
