package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ログイン試行の履歴を表す Entity（基本 immutable）。
 */
public class LoginHistory {

    private final Long id;
    private final AuthUserId authUserId;
    private final LocalDateTime loginAt;
    private final LoginResult result;

    private final LocalDateTime createdAt;
    private final LoginId createdBy;

    public LoginHistory(Long id,
                        AuthUserId authUserId,
                        LocalDateTime loginAt,
                        LoginResult result,
                        LocalDateTime createdAt,
                        LoginId createdBy) {

        this.id = id;
        this.authUserId = Objects.requireNonNull(authUserId, "authUserId must not be null");
        this.loginAt = Objects.requireNonNull(loginAt, "loginAt must not be null");
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static LoginHistory success(AuthUserId authUserId,
                                       LocalDateTime loginAt,
                                       LoginId createdBy) {
        return new LoginHistory(null, authUserId, loginAt, LoginResult.SUCCESS,
                loginAt, createdBy);
    }

    public static LoginHistory fail(AuthUserId authUserId,
                                    LocalDateTime loginAt,
                                    LoginId createdBy) {
        return new LoginHistory(null, authUserId, loginAt, LoginResult.FAIL,
                loginAt, createdBy);
    }

    public static LoginHistory locked(AuthUserId authUserId,
                                      LocalDateTime loginAt,
                                      LoginId createdBy) {
        return new LoginHistory(null, authUserId, loginAt, LoginResult.LOCKED,
                loginAt, createdBy);
    }

    public Long id() {
        return id;
    }

    public AuthUserId authUserId() {
        return authUserId;
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

    public LoginId createdBy() {
        return createdBy;
    }
}
