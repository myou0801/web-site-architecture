package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.List;

/**
 * 管理者によるアカウント管理の sharedService。
 */
public interface AuthAccountAdminSharedService {

    /**
     * アカウントを新規登録する。
     *
     * @param loginId   ログインID
     * @param rawPassword 初期パスワード（平文）
     * @param roleCodes 付与するロール一覧
     * @param operator  操作ユーザ（管理者）の loginId
     * @return 登録された AuthUser
     */
    AuthUser registerAccount(LoginId loginId,
                             String rawPassword,
                             List<RoleCode> roleCodes,
                             LoginId operator);

    /**
     * 初期パスワードにリセットし、ロックも解除する。
     *
     * @param targetUserId 対象ユーザID
     * @param operator     操作ユーザ（管理者）の loginId
     */
    void resetPasswordToInitial(AuthUserId targetUserId, LoginId operator);

    /**
     * アカウントロックを解除する。
     */
    void unlockAccount(AuthUserId targetUserId, LoginId operator);

    /**
     * アカウントを無効化する。
     */
    void disableAccount(AuthUserId targetUserId, LoginId operator);

    /**
     * アカウントを有効化する。
     */
    void enableAccount(AuthUserId targetUserId, LoginId operator);
}
