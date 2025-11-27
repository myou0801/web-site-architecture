package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

/**
 * ログイン中ユーザのパスワード変更を行う sharedService。
 */
public interface PasswordChangeSharedService {

    /**
     * パスワード強制変更のタイプを取得する。
     * @param loginId ログインID
     * @return パスワード強制変更のタイプ
     */
    PasswordChangeRequirementType requirementOf(LoginId loginId);

    /**
     * パスワード変更強制が必要かどうか判定する。
     * - 履歴なし → 強制（安全側）
     * - 履歴が INITIAL_REGISTER / ADMIN_RESET → 強制
     * - 履歴が USER_CHANGE で、有効期限切れ → 強制
     * @param loginId ログインID
     */
    default boolean isPasswordChangeRequired(LoginId loginId){
        return requirementOf(loginId).requiresPasswordChange();
    }

    /**
     * ログイン中ユーザのパスワードを変更する。
     * @param userId ユーザID
     * @param currentRawPassword 現在のパスワード（平文）
     * @param newRawPassword     新しいパスワード（平文）
     */
    void changePassword(AuthUserId userId, String currentRawPassword, String newRawPassword);
}
