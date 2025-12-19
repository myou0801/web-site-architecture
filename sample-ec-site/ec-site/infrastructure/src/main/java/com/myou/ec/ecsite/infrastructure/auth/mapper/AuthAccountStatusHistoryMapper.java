package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountStatusHistoryRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthAccountStatusHistoryMapper {
    void insert(AuthAccountStatusHistoryRecord record);
}
