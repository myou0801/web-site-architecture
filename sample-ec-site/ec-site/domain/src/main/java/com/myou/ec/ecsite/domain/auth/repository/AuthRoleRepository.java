package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthRole;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.List;

/**
 * ロールマスタおよびユーザロール関連の永続化インタフェース。
 */
public interface AuthRoleRepository {

    /**
     * 全ロール一覧を取得する。
     */
    List<AuthRole> findAll();

    /**
     * ユーザに紐づくロールコード一覧を取得する。
     */
    List<RoleCode> findRoleCodesByAccountId(AuthAccountId authAccountId);

    /**
     * ユーザに紐づくロールを差し替える。
     */
    void saveAccountRoles(AuthAccountId authAccountId, List<RoleCode> roleCodes);
}
