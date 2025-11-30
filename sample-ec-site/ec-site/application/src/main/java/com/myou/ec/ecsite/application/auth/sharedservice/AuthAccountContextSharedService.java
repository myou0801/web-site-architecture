package com.myou.ec.ecsite.application.auth.sharedservice;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 現在ログイン中のアカウント情報にアクセスする sharedService。
 */
public interface AuthAccountContextSharedService {

    /**
     * 現在ログイン中の AuthAccount を返す（存在しない場合は empty）。
     */
    Optional<AuthAccount> findCurrentUser();

    /**
     * 現在ログイン中の AuthAccount を返す（存在しない場合は例外）。
     */
    AuthAccount getCurrentUserOrThrow();

    /**
     * 現在ログイン中アカウントの前回ログイン日時（直近 SUCCESS）の値を返す。
     * ログイン履歴が不足している場合は empty。
     */
    Optional<LocalDateTime> findPreviousLoginAt();


}
