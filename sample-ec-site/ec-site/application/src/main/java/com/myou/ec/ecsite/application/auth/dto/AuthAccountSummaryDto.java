package com.myou.ec.ecsite.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 認証アカウントの概要情報を表すDTO。
 */
@Setter
@Getter
public class AuthAccountSummaryDto {
    /**
     * 認証アカウントID。
     */
    private Long authAccountId;
    /**
     * ユーザーID。
     */
    private String userId;
    /**
     * アカウントステータス。
     */
    private String accountStatus;
    /**
     * アカウントがロックされているかどうか。
     */
    private boolean locked;
    /**
     * アカウントが期限切れかどうか。
     */
    private boolean expired;
    /**
     * 作成日時。
     */
    private LocalDateTime createdAt;
    /**
     * 更新日時。
     */
    private LocalDateTime updatedAt;
    /**
     * 最終ログイン日時。
     */
    private LocalDateTime lastLoginAt;
    /**
     * ロールコードのセット。
     */
    private Set<String> roleCodes;

}
