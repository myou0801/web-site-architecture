package com.myou.ec.ecsite.application.auth.dto;

import java.util.List;

public class AuthAccountSearchRequest {
    public String userIdPrefix;
    public List<String> accountStatuses;
    public Boolean locked;
    public Boolean expired;
    public Page page;
    public Sort sort;

    public static class Page {
        public int pageNumber;
        public int pageSize;
    }

    public static class Sort {
        public String sortKey;
        public String direction; // ASC or DESC
    }
}
