package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthRole;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;

public record AuthRoleRecord(
        String roleCode,
        String roleName,
        String description,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {

    public AuthRole toDomain() {
        return new AuthRole(
                new RoleCode(roleCode),
                roleName,
                description
        );
    }
}
