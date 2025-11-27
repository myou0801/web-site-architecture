package com.myou.ec.ecsite.domain.auth.model.policy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * シンプルな固定ルールのパスワードポリシー実装。
 *
 * - 最小桁数
 * - 英数字のみ
 * - ログインIDとの完全一致禁止
 * - 有効期限（日数）
 * - 再利用禁止世代数
 */
public class BasicPasswordPolicy  {

    // パスワードの長さ（最低）
    private static final int MIN_LENGTH = 5;

    // パスワード有効期限
    private static final long EXPIRE_DAYS = 90;

    // 世代数
    private static final int HSTORY_GENERATION_COUNT = 3;



//    public void validateSyntax(String rawPassword, LoginId loginId) {
//
//        if (rawPassword == null || rawPassword.isBlank()) {
//            throw new PasswordPolicyViolationException("パスワードが未入力です。");
//        }
//
//        // 最小桁数
//        if (rawPassword.length() < MIN_LENGTH) {
//            throw new PasswordPolicyViolationException(
//                    "パスワードは" + MIN_LENGTH + "文字以上で入力してください。");
//        }
//
//        // 英数字のみ
//        if (!rawPassword.matches("^[0-9A-Za-z]+$")) {
//            throw new PasswordPolicyViolationException(
//                    "パスワードは英数字のみ利用できます。");
//        }
//
//        // ログインIDと完全一致禁止
//        if (loginId != null && rawPassword.equals(loginId.value())) {
//            throw new PasswordPolicyViolationException(
//                    "ログインIDと同じパスワードは利用できません。");
//        }
//    }


    public boolean isExpired(LocalDateTime lastChangedAt, LocalDateTime now) {
        if (lastChangedAt == null) {
            // 安全側：不明なら期限切れ扱い
            return true;
        }

        LocalDate changedDate = lastChangedAt.toLocalDate();
        LocalDate nowDate = now.toLocalDate();
        long days = ChronoUnit.DAYS.between(changedDate, nowDate);

        return days >= EXPIRE_DAYS;
    }


    public int historyGenerationCount() {
        return HSTORY_GENERATION_COUNT;
    }
}
