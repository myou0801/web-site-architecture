package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 現在ログイン中のユーザ情報にアクセスする sharedService。
 */
public interface AuthUserContextSharedService {

    /**
     * 現在ログイン中の AuthUser を返す（存在しない場合は empty）。
     */
    Optional<AuthUser> findCurrentUser();

    /**
     * 現在ログイン中の AuthUser を返す（存在しない場合は例外）。
     */
    AuthUser getCurrentUserOrThrow();

    /**
     * 現在ログイン中ユーザの前回ログイン日時（直近 SUCCESS）の値を返す。
     * ログイン履歴が不足している場合は empty。
     */
    Optional<LocalDateTime> findPreviousLoginAt();

    /**
     * 現在ログイン中ユーザのロール一覧（RoleCode）を返す。
     */
    List<RoleCode> getCurrentUserRoles();

    /**
     * 指定ロールを保持しているか（Spring Security の権限ではなく、DB上のロール）。
     */
    boolean hasRole(RoleCode roleCode);
}
