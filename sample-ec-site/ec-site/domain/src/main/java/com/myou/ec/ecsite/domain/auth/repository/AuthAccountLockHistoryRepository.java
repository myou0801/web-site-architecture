package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.AccountLockEvents;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

/**
 * アカウントロック／解除履歴の永続化インタフェース。
 */
public interface AuthAccountLockHistoryRepository {

    void save(AccountLockEvent event, UserId operator);

    /**
     * 対象ユーザのロック／解除イベント一覧を取得。
     * 時系列のソートは infrastructure / AccountLockEvents 側で正規化する前提。
     */
    AccountLockEvents findByAccountId(AuthAccountId accountId, int limit);

}
