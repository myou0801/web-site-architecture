package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthAccountMapper {
    AuthAccountRecord selectByAccountId(@Param("authAccountId") long authAccountId);
    AuthAccountRecord selectByLoginId(@Param("loginId") String loginId);
    void insert(AuthAccountRecord record);
    void update(AuthAccountRecord record);
}
