package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.util.List;

/**
 * 管理者によるアカウント管理の sharedService。
 */
public interface AuthAccountAdminSharedService {

    /**
     * アカウントを新規登録する。
     *
     * @param newUserId    作成ユーザーID
     * @param roleCodes 付与するロール一覧
     * @param operator  操作ユーザ（管理者）の userId
     * @return 登録された ユーザーID
     */
    AuthAccountId registerAccount(UserId newUserId,
                                  List<RoleCode> roleCodes,
                                  UserId operator);

    /**
     * 初期パスワードにリセットする。
     *
     * @param targetAccountId 対象アカウントID
     * @param operator        操作ユーザ（管理者）の userId
     */
    void resetPassword(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントロックを解除する。
     *
     * @param targetAccountId 対象アカウントID
     * @param operator        操作ユーザ（管理者）の userId
     */
    void unlockAccount(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントを無効化する。
     *
     * @param targetAccountId 対象アカウントID
     * @param operator        操作ユーザ（管理者）の userId
     */
    void disableAccount(AuthAccountId targetAccountId, UserId operator);

    /**
     * アカウントを有効化する。
     *
     * @param targetAccountId 対象アカウントID
     * @param operator        操作ユーザ（管理者）の userId
     */
    void enableAccount(AuthAccountId targetAccountId, UserId operator);


    /** ロール付与 */
    void addRole(AuthAccountId targetAccountId, RoleCode role, UserId operator);

    /** ロール剥奪 */
    void removeRole(AuthAccountId targetAccountId, RoleCode role, UserId operator);

    /** 論理削除（deleted=true, enabled=false） */
    void deleteAccount(AuthAccountId targetAccountId, UserId operator);
}
