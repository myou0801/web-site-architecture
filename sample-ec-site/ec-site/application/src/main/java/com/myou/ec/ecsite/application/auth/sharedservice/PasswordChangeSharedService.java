package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

/**
 * ログイン中ユーザのパスワード変更を行う sharedService。
 */
public interface PasswordChangeSharedService {

    /**
     * パスワード強制変更のタイプを取得する。
     * @param userId ユーザーID
     * @return パスワード強制変更のタイプ
     */
    PasswordChangeRequirementType requirementOf(UserId userId);

    /**
     * パスワード変更強制が必要かどうか判定する。
     * - 履歴なし → 強制（安全側）
     * - 履歴が INITIAL_REGISTER / ADMIN_RESET → 強制
     * - 履歴が USER_CHANGE で、有効期限切れ → 強制
     * @param userId ユーザーID
     */
    default boolean isPasswordChangeRequired(UserId userId){
        return requirementOf(userId).requiresPasswordChange();
    }

    /**
     * ログイン中ユーザのパスワードを変更する。
     * @param accountId アカウントID
     * @param currentRawPassword 現在のパスワード（平文）
     * @param newRawPassword     新しいパスワード（平文）
     */
    void changePassword(AuthAccountId accountId, String currentRawPassword, String newRawPassword);
}
