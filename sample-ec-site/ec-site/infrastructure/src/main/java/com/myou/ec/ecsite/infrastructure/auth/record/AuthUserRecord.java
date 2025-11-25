package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AUTH_USER テーブルの1行を表す Record。
 */
public record AuthUserRecord(
        Long authUserId,
        String loginId,
        String loginPassword,
        boolean enabled,
        boolean deleted,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        long versionNo
) {

    public AuthUser toDomain(List<RoleCode> roleCodes) {
        return new AuthUser(
                authUserId != null ? new AuthUserId(authUserId) : null,
                new LoginId(loginId),
                new EncodedPassword(loginPassword),
                enabled,
                deleted,
                roleCodes,
                createdAt,
                new LoginId(createdBy),
                updatedAt,
                new LoginId(updatedBy),
                versionNo
        );
    }

    public static AuthUserRecord fromDomain(AuthUser user) {
        Long id = user.id() != null ? user.id().value() : null;
        return new AuthUserRecord(
                id,
                user.loginId().value(),
                user.encodedPassword().value(),
                user.enabled(),
                user.deleted(),
                user.createdAt(),
                user.createdByLoginId().value(),
                user.updatedAt(),
                user.updatedByLoginId().value(),
                user.versionNo()
        );
    }
}
