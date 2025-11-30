package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.util.Optional;

/**
 * 認証アカウントの永続化インタフェース。
 */
public interface AuthAccountRepository {

    Optional<AuthAccount> findById(AuthAccountId id);

    Optional<AuthAccount> findByUserId(UserId userId);

    void save(AuthAccount user);

}
