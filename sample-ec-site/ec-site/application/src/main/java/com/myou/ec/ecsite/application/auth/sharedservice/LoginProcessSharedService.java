package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

/**
 * ログイン成功/失敗時のドメイン処理（履歴記録・ロック制御など）を扱う sharedService。
 * presentation（Handler）から呼び出される。
 */
public interface LoginProcessSharedService {

    /**
     * ログイン成功時の処理。
     *
     * @param loginId   ログインID（認証成功済）
     */
    void onLoginSuccess(LoginId loginId);

    /**
     * ログイン失敗時の処理。
     *
     * @param loginId   ログインID（フォーム入力値。存在しない場合もある）
     */
    void onLoginFailure(LoginId loginId);


    /**
     * パスワード変更強制が必要かどうか判定する。
     * - 履歴なし → 強制（安全側）
     * - 履歴が INITIAL_REGISTER / ADMIN_RESET → 強制
     * - 履歴が USER_CHANGE で、有効期限切れ → 強制
     * @param loginId ログインID
     */
    boolean isPasswordChangeRequired(LoginId loginId);
}

