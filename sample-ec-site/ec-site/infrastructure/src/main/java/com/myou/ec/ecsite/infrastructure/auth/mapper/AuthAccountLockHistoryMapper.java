package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthAccountLockHistoryMapper {

    void insert(AuthAccountLockHistoryRecord record);

    List<AuthAccountLockHistoryRecord> findByUserId(@Param("authUserId") long authUserId,
                                                    @Param("limit") int limit);
}
