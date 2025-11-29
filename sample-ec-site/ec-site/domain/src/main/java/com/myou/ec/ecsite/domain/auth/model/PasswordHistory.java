package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * パスワード変更履歴 Entity（immutable）。
 */
public class PasswordHistory {

    private final Long id;
    private final AuthAccountId authAccountId;
    private final EncodedPassword encodedPassword;
    private final PasswordChangeType changeType;
    private final LocalDateTime changedAt;
    private final UserId changedBy;
    private final LocalDateTime createdAt;
    private final UserId createdBy;

    public PasswordHistory(Long id,
                           AuthAccountId authAccountId,
                           EncodedPassword encodedPassword,
                           PasswordChangeType changeType,
                           LocalDateTime changedAt,
                           UserId changedBy,
                           LocalDateTime createdAt,
                           UserId createdBy) {

        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId, "authAccountId must not be null");
        this.encodedPassword = Objects.requireNonNull(encodedPassword, "encodedPassword must not be null");
        this.changeType = Objects.requireNonNull(changeType, "changeType must not be null");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static PasswordHistory initialRegister(AuthAccountId authAccountId,
                                                  EncodedPassword password,
                                                  LocalDateTime now,
                                                  UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.INITIAL_REGISTER, now, operator, now, operator);
    }

    public static PasswordHistory adminReset(AuthAccountId authAccountId,
                                             EncodedPassword password,
                                             LocalDateTime now,
                                             UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.ADMIN_RESET, now, operator, now, operator);
    }

    public static PasswordHistory userChange(AuthAccountId authAccountId,
                                             EncodedPassword password,
                                             LocalDateTime now,
                                             UserId operator) {
        return new PasswordHistory(null, authAccountId, password,
                PasswordChangeType.USER_CHANGE, now, operator, now, operator);
    }

    /**
     * パスワードの強制変更が必要かを判断する
     * @return パスワードの変更が必要か否か
     */
    public boolean isPasswordChangeRequired(){
        return changeType == PasswordChangeType.INITIAL_REGISTER
                || changeType == PasswordChangeType.ADMIN_RESET;
    }

    public Long id() {
        return id;
    }

    public AuthAccountId authAccountId() {
        return authAccountId;
    }

    public EncodedPassword encodedPassword() {
        return encodedPassword;
    }

    public PasswordChangeType changeType() {
        return changeType;
    }

    public LocalDateTime changedAt() {
        return changedAt;
    }

    public UserId changedBy() {
        return changedBy;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdBy() {
        return createdBy;
    }
}
