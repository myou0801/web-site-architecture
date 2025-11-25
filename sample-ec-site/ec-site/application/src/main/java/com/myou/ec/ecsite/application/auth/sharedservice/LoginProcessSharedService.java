package com.myou.ec.ecsite.application.auth.sharedservice;

/**
 * ログイン成功/失敗時のドメイン処理（履歴記録・ロック制御など）を扱う sharedService。
 * presentation（Handler）から呼び出される。
 */
public interface LoginProcessSharedService {

    /**
     * ログイン成功時の処理。
     *
     * @param loginId   ログインID（認証成功済）
     * @return ログイン成功結果（パスワード変更強制が必要かどうか）
     */
    LoginSuccessResult onLoginSuccess(String loginId);

    /**
     * ログイン失敗時の処理。
     *
     * @param loginId   ログインID（フォーム入力値。存在しない場合もある）
     * @return 失敗種類（ロック or 認証エラー）
     */
    LoginFailureType onLoginFailure(String loginId);
}
