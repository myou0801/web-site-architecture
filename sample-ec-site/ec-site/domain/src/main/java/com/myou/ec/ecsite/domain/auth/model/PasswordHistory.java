package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * パスワード変更履歴 Entity（immutable）。
 */
public class PasswordHistory {

    private final Long id;
    private final AuthUserId authUserId;
    private final EncodedPassword encodedPassword;
    private final PasswordChangeType changeType;
    private final LocalDateTime changedAt;
    private final LoginId changedBy;
    private final LocalDateTime createdAt;
    private final LoginId createdBy;

    public PasswordHistory(Long id,
                           AuthUserId authUserId,
                           EncodedPassword encodedPassword,
                           PasswordChangeType changeType,
                           LocalDateTime changedAt,
                           LoginId changedBy,
                           LocalDateTime createdAt,
                           LoginId createdBy) {

        this.id = id;
        this.authUserId = Objects.requireNonNull(authUserId, "authUserId must not be null");
        this.encodedPassword = Objects.requireNonNull(encodedPassword, "encodedPassword must not be null");
        this.changeType = Objects.requireNonNull(changeType, "changeType must not be null");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static PasswordHistory initialRegister(AuthUserId authUserId,
                                                  EncodedPassword password,
                                                  LocalDateTime now,
                                                  LoginId operator) {
        return new PasswordHistory(null, authUserId, password,
                PasswordChangeType.INITIAL_REGISTER, now, operator, now, operator);
    }

    public static PasswordHistory adminReset(AuthUserId authUserId,
                                             EncodedPassword password,
                                             LocalDateTime now,
                                             LoginId operator) {
        return new PasswordHistory(null, authUserId, password,
                PasswordChangeType.ADMIN_RESET, now, operator, now, operator);
    }

    public static PasswordHistory userChange(AuthUserId authUserId,
                                             EncodedPassword password,
                                             LocalDateTime now,
                                             LoginId operator) {
        return new PasswordHistory(null, authUserId, password,
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

    public AuthUserId authUserId() {
        return authUserId;
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

    public LoginId changedBy() {
        return changedBy;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LoginId createdBy() {
        return createdBy;
    }
}
