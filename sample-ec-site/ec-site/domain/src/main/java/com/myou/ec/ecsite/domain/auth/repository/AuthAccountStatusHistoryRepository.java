package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccountStatusHistory;
import com.myou.ec.ecsite.domain.auth.model.value.Operator; // Import Operator


/**
 * アカウント状態変更履歴リポジトリ。
 */
public interface AuthAccountStatusHistoryRepository {
    void save(AuthAccountStatusHistory history, Operator operator);
}
