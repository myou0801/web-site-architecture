package com.myou.ec.ecsite.domain.auth.policy;

import com.myou.ec.ecsite.domain.auth.model.LoginHistories;

import java.time.LocalDateTime;

public interface LockPolicy {

    /**
     * ロックアウトすべきかどうかを判定する。
     *
     * @param histories         対象ユーザのログイン履歴（新しいものから順）
     * @param boundaryExclusive この日時より前の履歴は判定対象外（最後の UNLOCK 時刻など）。不要なら null。
     */
    boolean isLockout(LoginHistories histories, LocalDateTime boundaryExclusive);

}
