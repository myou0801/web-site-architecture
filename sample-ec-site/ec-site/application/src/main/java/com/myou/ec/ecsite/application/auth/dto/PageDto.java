package com.myou.ec.ecsite.application.auth.dto;

import java.util.List;

public class PageDto<T> {
    public List<T> items;
    public long totalCount;
    public int pageNumber;
    public int pageSize;
}
