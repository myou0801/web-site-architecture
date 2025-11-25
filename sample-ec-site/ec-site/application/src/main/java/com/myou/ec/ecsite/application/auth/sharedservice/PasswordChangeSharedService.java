package com.myou.ec.ecsite.application.auth.sharedservice;

/**
 * ログイン中ユーザのパスワード変更を行う sharedService。
 */
public interface PasswordChangeSharedService {

    /**
     * ログイン中ユーザのパスワードを変更する。
     *
     * @param currentRawPassword 現在のパスワード（平文）
     * @param newRawPassword     新しいパスワード（平文）
     */
    void changePasswordOfCurrentUser(String currentRawPassword, String newRawPassword);
}
