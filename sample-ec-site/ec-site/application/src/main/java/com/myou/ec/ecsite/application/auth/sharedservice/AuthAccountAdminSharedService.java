package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.util.Set;

/**
 * 管理者によるアカウント管理の sharedService。
 */
public interface AuthAccountAdminSharedService {

    /**
     * アカウントを新規登録する。
     *
     * @param newLoginId 作成ログインID
     * @param roleCodes 付与するロール一覧

     * @return 登録された ログインID
     */
    AuthAccountId registerAccount(LoginId newLoginId,
                                  Set<RoleCode> roleCodes);

    /**
     * 初期パスワードにリセットする。
     *
     * @param targetAccountId 対象アカウントID
     */
    void resetPassword(AuthAccountId targetAccountId);

    /**
     * アカウントロックを解除する。
     *
     * @param targetAccountId 対象アカウントID
     */
    void unlockAccount(AuthAccountId targetAccountId);

    /**
     * アカウントを無効化する。
     *
     * @param targetAccountId 対象アカウントID
     */
    void disableAccount(AuthAccountId targetAccountId);

    /**
     * アカウントを有効化する。
     *
     * @param targetAccountId 対象アカウントID
     */
    void enableAccount(AuthAccountId targetAccountId);


    /** ロール付与 */
    void addRole(AuthAccountId targetAccountId, RoleCode role);

    /** ロール剥奪 */
    void removeRole(AuthAccountId targetAccountId, RoleCode role);

    /** 論理削除（deleted=true, enabled=false） */
    void deleteAccount(AuthAccountId targetAccountId);
}
