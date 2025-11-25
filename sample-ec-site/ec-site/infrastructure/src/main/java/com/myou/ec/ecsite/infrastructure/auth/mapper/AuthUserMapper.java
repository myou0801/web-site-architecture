package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthUserMapper {

    AuthUserRecord findById(@Param("authUserId") long authUserId);

    AuthUserRecord findByLoginId(@Param("loginId") String loginId);

    void insert(AuthUserRecord record);

    void update(AuthUserRecord record);
}
