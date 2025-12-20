package com.myou.ec.ecsite.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 認証アカウント検索パラメータを表すDTO。
 */
@Setter
@Getter
public class AuthAccountSearchParam {
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
     * ソートキー。
     */
    private String sortKey;
    /**
     * ソート方向。
     */
    private String sortDirection;
    /**
     * 取得件数。
     */
    private int limit;
    /**
     * オフセット。
     */
    private int offset;

}
