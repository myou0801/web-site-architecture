package com.myou.ec.ecsite.application.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class AuthAccountSummaryDto {
    public Long authAccountId;
    public String userId;
    public String accountStatus;
    public boolean locked;
    public boolean expired;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime lastLoginAt;
    public Set<String> roleCodes;
}
