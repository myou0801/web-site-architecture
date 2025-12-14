package com.myou.ec.ecsite.domain.auth.policy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ExpiredPasswordPolicy {

    // パスワード有効期限
    private static final long EXPIRE_DAYS = 90;

    private static final int DORMACNY_DAYS = 15;


    private final LocalDateTime lastSuccessAt;

    public ExpiredPasswordPolicy(LocalDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public boolean isExpired(LocalDateTime now) {
        if (lastSuccessAt == null) {
            // 安全側：不明なら期限切れ扱い
            return true;
        }

        LocalDate changedDate = lastSuccessAt.toLocalDate();
        LocalDate nowDate = now.toLocalDate();
        long days = ChronoUnit.DAYS.between(changedDate, nowDate);

        return days >= EXPIRE_DAYS;
    }

    public boolean validateDormancyDays(LocalDateTime now) {
        var threshold = now.minusDays(DORMACNY_DAYS);
        return !lastSuccessAt.isBefore(threshold);
    }
}
