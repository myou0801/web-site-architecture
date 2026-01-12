package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.Operator; // Import Operator
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.util.Optional;

/**
 * 認証アカウントの永続化インタフェース。
 */
public interface AuthAccountRepository {

    Optional<AuthAccount> findById(AuthAccountId id);

    Optional<AuthAccount> findByLoginId(LoginId loginId);

    void save(AuthAccount authAccount, Operator operator);

}
