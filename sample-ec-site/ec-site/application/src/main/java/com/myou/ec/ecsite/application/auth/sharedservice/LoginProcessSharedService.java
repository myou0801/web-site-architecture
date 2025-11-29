package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.UserId;

/**
 * ログイン成功/失敗時のドメイン処理（履歴記録・ロック制御など）を扱う sharedService。
 * presentation（Handler）から呼び出される。
 */
public interface LoginProcessSharedService {

    /**
     * ログイン成功時の処理。
     *
     * @param userId   ユーザID（認証成功済）
     */
    void onLoginSuccess(UserId userId);

    /**
     * ログイン失敗時の処理。
     *
     * @param userId   ユーザID（フォーム入力値。存在しない場合もある）
     */
    void onLoginFailure(UserId userId);



}

