package com.myou.ec.ecsite.application.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 認証アカウントとロールの関連を表すDTO。
 */

@Setter
@Getter
public class AuthAccountRoleDto {

    /**
     * 認証アカウントID。
     */

    private Long authAccountId;

    /**
     * ロールコード。
     */

    private String roleCode;


}
