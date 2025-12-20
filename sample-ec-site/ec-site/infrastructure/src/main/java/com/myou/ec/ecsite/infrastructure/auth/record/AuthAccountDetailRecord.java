package com.myou.ec.ecsite.infrastructure.auth.record;

import java.time.LocalDateTime;

public class AuthAccountDetailRecord {
    public Long authAccountId;
    public String userId;
    public String accountStatus;
    public Boolean locked;
    public Boolean expired;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime lastLoginAt;
}
