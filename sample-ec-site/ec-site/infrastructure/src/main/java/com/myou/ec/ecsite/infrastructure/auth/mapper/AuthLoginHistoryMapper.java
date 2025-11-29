package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthLoginHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthLoginHistoryMapper {

    void insert(AuthLoginHistoryRecord record);

    List<AuthLoginHistoryRecord> findRecentByAccountId(@Param("authAccountId") long authAccountId,
                                                    @Param("limit") int limit);

    AuthLoginHistoryRecord findPreviousSuccessLoginAtByAccountId(@Param("authAccountId") long authAccountId);

}
