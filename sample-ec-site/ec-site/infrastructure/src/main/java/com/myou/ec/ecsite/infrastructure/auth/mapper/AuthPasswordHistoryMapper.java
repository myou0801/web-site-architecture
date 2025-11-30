package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthPasswordHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthPasswordHistoryMapper {

    void insert(AuthPasswordHistoryRecord record);

    List<AuthPasswordHistoryRecord> selectRecentByAccountId(@Param("authAccountId") long authAccountId,
                                                            @Param("limit") int limit);

    AuthPasswordHistoryRecord selectLatestByAccountId(@Param("authAccountId") long authAccountId);
}
