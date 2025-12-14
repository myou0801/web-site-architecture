package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ログイン試行の履歴を表す Entity（基本 immutable）。
 */
public record LoginHistory(Long id, AuthAccountId authAccountId, LoginResult result, LocalDateTime loginAt,
                           LocalDateTime createdAt, UserId createdBy) {

    public LoginHistory(Long id,
                        AuthAccountId authAccountId,
                        LoginResult result,
                        LocalDateTime loginAt,
                        LocalDateTime createdAt,
                        UserId createdBy) {

        this.id = id;
        this.authAccountId = Objects.requireNonNull(authAccountId, "authAccountId must not be null");
        this.loginAt = Objects.requireNonNull(loginAt, "loginAt must not be null");
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static LoginHistory success(AuthAccountId authAccountId,
                                       LocalDateTime loginAt,
                                       UserId createdBy) {
        return new LoginHistory(null, authAccountId, LoginResult.SUCCESS, loginAt,
                loginAt, createdBy);
    }

    public static LoginHistory fail(AuthAccountId authAccountId,
                                    LocalDateTime loginAt,
                                    UserId createdBy) {
        return new LoginHistory(null, authAccountId, LoginResult.FAILURE, loginAt,
                loginAt, createdBy);
    }

    public static LoginHistory locked(AuthAccountId authAccountId,
                                      LocalDateTime loginAt,
                                      UserId createdBy) {
        return new LoginHistory(null, authAccountId, LoginResult.LOCKED, loginAt,
                loginAt, createdBy);
    }

    public static LoginHistory disabled(AuthAccountId authAccountId,
                                        LocalDateTime loginAt,
                                        UserId createdBy) {
        return new LoginHistory(null, authAccountId, LoginResult.DISABLED, loginAt,
                loginAt, createdBy);
    }

    public static LoginHistory expired(AuthAccountId authAccountId,
                                       LocalDateTime loginAt,
                                       UserId createdBy) {
        return new LoginHistory(null, authAccountId, LoginResult.EXPIRED, loginAt,
                loginAt, createdBy);
    }
}
