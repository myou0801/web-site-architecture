package com.myou.ec.ecsite.infrastructure.auth.record;

public record AuthAccountRoleRecord(
        Long authAccountId,
        String roleCode
) {
}
