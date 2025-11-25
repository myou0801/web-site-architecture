package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthPasswordHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthPasswordHistoryMapper {

    void insert(AuthPasswordHistoryRecord record);

    List<AuthPasswordHistoryRecord> findRecentByUserId(@Param("authUserId") long authUserId,
                                                       @Param("limit") int limit);

    AuthPasswordHistoryRecord findLastByUserId(@Param("authUserId") long authUserId);
}
