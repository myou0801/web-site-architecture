package com.myou.ec.ecsite.application.auth.dto;

import java.util.List;

public class AuthAccountSearchParam {
    public String userIdPrefix;
    public List<String> accountStatuses;
    public Boolean locked;
    public Boolean expired;
    public String sortKey;
    public String sortDirection;
    public int limit;
    public int offset;
}
