package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthRoleRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthRoleMapper {

    List<AuthRoleRecord> findAll();

    List<String> findRoleCodesByAccountId(@Param("authAccountId") long authAccountId);

    void deleteUserRoles(@Param("authAccountId") long authAccountId);

    void insertUserRole(@Param("authAccountId") long authAccountId,
                        @Param("roleCode") String roleCode);
}
