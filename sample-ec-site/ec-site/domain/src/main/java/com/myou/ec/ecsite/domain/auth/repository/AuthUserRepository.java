package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.util.Optional;

/**
 * 認証ユーザの永続化インタフェース。
 */
public interface AuthUserRepository {

    Optional<AuthUser> findById(AuthUserId id);

    Optional<AuthUser> findByLoginId(LoginId loginId);

    void save(AuthUser user);
}
