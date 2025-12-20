package com.myou.ec.ecsite.infrastructure.auth.mapper;


import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRoleRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthAccountRoleQueryMapper {

    List<AuthAccountRoleRecord> selectRoleCodesByAccountIds(@Param("authAccountIds") List<Long> authAccountIds);

    List<String> selectRoleCodesByAccountId(@Param("authAccountId") Long authAccountId);
}
