package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.Objects;

/**
 * ロールマスタ Entity。
 * 画面表示・権限管理用のマスタ。
 */
public class AuthRole {

    private final RoleCode roleCode;
    private final String roleName;
    private final String description;

    public AuthRole(RoleCode roleCode, String roleName, String description) {
        this.roleCode = Objects.requireNonNull(roleCode, "roleCode must not be null");
        this.roleName = Objects.requireNonNull(roleName, "roleName must not be null");
        this.description = description;
    }

    public RoleCode roleCode() {
        return roleCode;
    }

    public String roleName() {
        return roleName;
    }

    public String description() {
        return description;
    }
}
