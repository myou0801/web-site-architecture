package com.myou.ec.ecsite.domain.auth.policy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ExpiredPasswordPolicy {

    // パスワード有効期限
    private static final long EXPIRE_DAYS = 90;

    private final LocalDateTime now;

    public ExpiredPasswordPolicy(LocalDateTime now) {
        this.now = now;
    }

    public boolean isExpired(LocalDateTime lastChangedAt) {
        if (lastChangedAt == null) {
            // 安全側：不明なら期限切れ扱い
            return true;
        }

        LocalDate changedDate = lastChangedAt.toLocalDate();
        LocalDate nowDate = now.toLocalDate();
        long days = ChronoUnit.DAYS.between(changedDate, nowDate);

        return days >= EXPIRE_DAYS;
    }
}
