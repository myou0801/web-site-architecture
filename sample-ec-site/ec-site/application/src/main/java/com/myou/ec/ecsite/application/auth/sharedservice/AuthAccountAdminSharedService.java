package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.List;

/**
 * 管理者によるアカウント管理の sharedService。
 */
public interface AuthAccountAdminSharedService {

    /**
     * アカウントを新規登録する。
     *
     * @param userId   ユーザーID
     * @param roleCodes 付与するロール一覧
     * @param operator  操作ユーザ（管理者）の userId
     * @return 登録された ユーザーID
     */
    AuthAccountId registerAccount(UserId userId,
                             List<RoleCode> roleCodes,
                             UserId operator);

    /**
     * 初期パスワードにリセットし、ロックも解除する。
     *
     * @param targetAccountId 対象アカウントID
     * @param operator     操作ユーザ（管理者）の userId
     */
    void resetPasswordToInitial(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントロックを解除する。
     */
    void unlockAccount(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントを無効化する。
     */
    void disableAccount(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントを有効化する。
     */
    void enableAccount(AuthAccountId targetAccountId, UserId operator);
}
