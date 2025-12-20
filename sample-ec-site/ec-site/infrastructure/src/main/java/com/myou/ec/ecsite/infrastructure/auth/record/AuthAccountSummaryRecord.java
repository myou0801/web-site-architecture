package com.myou.ec.ecsite.infrastructure.auth.record;

import java.time.LocalDateTime;

public record AuthAccountSummaryRecord(
        Long authAccountId,
        String userId,
        String accountStatus,
        Boolean locked,
        Boolean expired,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {
}
