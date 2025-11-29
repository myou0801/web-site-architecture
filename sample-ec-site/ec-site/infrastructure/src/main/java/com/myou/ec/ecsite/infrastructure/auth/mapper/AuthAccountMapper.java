package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthAccountMapper {

    AuthAccountRecord findById(@Param("authAccountId") long authAccountId);

    AuthAccountRecord findByUserId(@Param("userId") String userId);

    void insert(AuthAccountRecord record);

    void update(AuthAccountRecord record);
}
