package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthAccountLockHistoryMapper {

    void insert(AuthAccountLockHistoryRecord record);

    List<AuthAccountLockHistoryRecord> findByAccountId(@Param("authAccountId") long authAccountId,
                                                    @Param("limit") int limit);
}
