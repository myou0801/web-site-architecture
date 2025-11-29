package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ログイン試行の履歴を表す Entity（基本 immutable）。
 */
public class LoginHistory {

    private final Long id;
    private final AuthAccountId authAccountId;
    private final LocalDateTime loginAt;
    private final LoginResult result;

    private final LocalDateTime createdAt;
    private final UserId createdBy;

    public LoginHistory(Long id,
                        AuthAccountId authAccountId,
                        LocalDateTime loginAt,
                        LoginResult result,
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
        return new LoginHistory(null, authAccountId, loginAt, LoginResult.SUCCESS,
                loginAt, createdBy);
    }

    public static LoginHistory fail(AuthAccountId authAccountId,
                                    LocalDateTime loginAt,
                                    UserId createdBy) {
        return new LoginHistory(null, authAccountId, loginAt, LoginResult.FAIL,
                loginAt, createdBy);
    }

    public static LoginHistory locked(AuthAccountId authAccountId,
                                      LocalDateTime loginAt,
                                      UserId createdBy) {
        return new LoginHistory(null, authAccountId, loginAt, LoginResult.LOCKED,
                loginAt, createdBy);
    }

    public static LoginHistory disabled(AuthAccountId authAccountId,
                                      LocalDateTime loginAt,
                                      UserId createdBy) {
        return new LoginHistory(null, authAccountId, loginAt, LoginResult.DISABLED,
                loginAt, createdBy);
    }

    public Long id() {
        return id;
    }

    public AuthAccountId authAccountId() {
        return authAccountId;
    }

    public LocalDateTime loginAt() {
        return loginAt;
    }

    public LoginResult result() {
        return result;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdBy() {
        return createdBy;
    }
}
