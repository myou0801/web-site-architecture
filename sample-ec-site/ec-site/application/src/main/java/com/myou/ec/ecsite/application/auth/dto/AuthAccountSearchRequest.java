package com.myou.ec.ecsite.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 認証アカウント検索リクエストを表すDTO。
 */
@Setter
@Getter
public class AuthAccountSearchRequest {
    /**
     * ユーザーID（前方一致）。
     */
    private String userIdPrefix;
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
    public static class Page {
        /**
         * ページ番号。
         */
        private int pageNumber;
        /**
         * 1ページあたりのサイズ。
         */
        private int pageSize;

        public int getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }

    /**
     * ソート情報を表す内部クラス。
     */
    public static class Sort {
        /**
         * ソートキー。
         */
        private String sortKey;
        /**
         * ソート方向 (ASC or DESC)。
         */
        private String direction;

        public String getSortKey() {
            return sortKey;
        }

        public void setSortKey(String sortKey) {
            this.sortKey = sortKey;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }
}
