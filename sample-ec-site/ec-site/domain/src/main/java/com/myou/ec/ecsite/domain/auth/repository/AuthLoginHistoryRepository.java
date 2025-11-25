package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistories;
import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;

import java.util.Optional;

/**
 * ログイン履歴の永続化インタフェース。
 */
public interface AuthLoginHistoryRepository {

    void save(LoginHistory history);

    LoginHistories findRecentByUserId(AuthUserId userId, int limit);

    Optional<LoginHistory> findPreviousSuccessLoginAt(AuthUserId userId);

}
