package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

import java.util.List;
import java.util.Optional;

/**
 * パスワード履歴の永続化インタフェース。
 */
public interface AuthPasswordHistoryRepository {

    void save(PasswordHistory history);

    List<PasswordHistory> findRecentByAccountId(AuthAccountId accountId, int limit);

    Optional<PasswordHistory> findLastByAccountId(AuthAccountId accountId);
}
