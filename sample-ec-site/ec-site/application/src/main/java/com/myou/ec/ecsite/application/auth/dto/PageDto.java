package com.myou.ec.ecsite.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * ページネーションされた結果を表す汎用DTO。
 *
 * @param <T> ページに含まれるアイテムの型
 */
@Setter
@Getter
public class PageDto<T> {
    /**
     * 現在のページのアイテムリスト。
     */
    private List<T> items;
    /**
     * 全アイテムの総数。
     */
    private long totalCount;
    /**
     * 現在のページ番号。
     */
    private int pageNumber;
    /**
     * 1ページあたりのアイテム数。
     */
    private int pageSize;

}
