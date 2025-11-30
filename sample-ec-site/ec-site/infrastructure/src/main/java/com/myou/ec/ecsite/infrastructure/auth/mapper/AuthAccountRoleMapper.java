package com.myou.ec.ecsite.infrastructure.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuthAccountRoleMapper {
    List<String> selectRoleCodesByAccountId(@Param("authAccountId") long authAccountId);

    int insert(
            @Param("authAccountId") long authAccountId,
            @Param("roleCode") String roleCode,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("createdBy") String createdBy
    );

    int delete(
            @Param("authAccountId") long authAccountId,
            @Param("roleCode") String roleCode
    );
}
