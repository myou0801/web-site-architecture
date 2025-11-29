package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistories;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

import java.util.Optional;

/**
 * ログイン履歴の永続化インタフェース。
 */
public interface AuthLoginHistoryRepository {

    void save(LoginHistory history);

    LoginHistories findRecentByAccountId(AuthAccountId accountId, int limit);

    Optional<LoginHistory> findPreviousSuccessLoginAtByAccountId(AuthAccountId accountId);

}
