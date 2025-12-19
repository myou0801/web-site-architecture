package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * パスワード変更履歴 Entity（immutable）。
 */
public record PasswordHistory(Long id, AuthAccountId authAccountId, PasswordHash passwordHash,
                              PasswordChangeType changeType, LocalDateTime changedAt, UserId operatedBy) {

    public PasswordHistory(Long id,
                           AuthAccountId authAccountId,
                           PasswordHash passwordHash,
                           PasswordChangeType changeType,
                           LocalDateTime changedAt,
                           UserId operatedBy) {

        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId, "authAccountId must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "encodedPassword must not be null");
        this.changeType = Objects.requireNonNull(changeType, "changeType must not be null");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        this.operatedBy = Objects.requireNonNull(operatedBy, "operatedBy must not be null");
    }

    public static PasswordHistory initialRegister(AuthAccountId authAccountId,
                                                  PasswordHash password,
                                                  LocalDateTime now,
                                                  UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.INITIAL_REGISTER, now, operator);
    }

    public static PasswordHistory adminReset(AuthAccountId authAccountId,
                                             PasswordHash password,
                                             LocalDateTime now,
                                             UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.ADMIN_RESET, now, operator);
    }

    public static PasswordHistory userChange(AuthAccountId authAccountId,
                                             PasswordHash password,
                                             LocalDateTime now,
                                             UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.USER_CHANGE, now, operator);
    }

    /**
     * パスワードの強制変更が必要かを判断する
     *
     * @return パスワードの変更が必要か否か
     */
    public boolean isPasswordChangeRequired() {
        return changeType == PasswordChangeType.INITIAL_REGISTER
                || changeType == PasswordChangeType.ADMIN_RESET;
    }
}
