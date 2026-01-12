package com.myou.ec.ecsite.application.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 認証アカウント検索リクエストを表すDTO。
 */
@Getter
@Builder
public class AuthAccountSearchRequest {
    /**
     * ログインID（前方一致）。
     */
    private String loginIdPrefix;
    /**
     * アカウントステータスのリスト。
     */
    private List<String> accountStatuses;
    /**
     * ロックされているかどうか。
     */
    private Boolean locked;
    /**
     * 期限切れかどうか。
     */
    private Boolean expired;
    /**
     * ページ情報。
     */
    private Page page;
    /**
     * ソート情報。
     */
    private Sort sort;

    /**
     * ページ情報を表す内部クラス。
     */
    @Getter
    @Builder
    public static class Page {
        /**
         * ページ番号。
         */
        private int pageNumber;
        /**
         * 1ページあたりのサイズ。
         */
        private int pageSize;

    }

    /**
     * ソート情報を表す内部クラス。
     */
    @Getter
    @Builder
    public static class Sort {
        /**
         * ソートキー。
         */
        private String sortKey;
        /**
         * ソート方向 (ASC or DESC)。
         */
        private String direction;

    }
}
