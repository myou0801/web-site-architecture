package com.myou.ec.ecsite.infrastructure.auth.mapper;


import com.myou.ec.ecsite.infrastructure.auth.record.AccountExpiryHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthAccountExpiryHistoryMapper {
    List<AccountExpiryHistoryRecord> selectByAccountId(@Param("authAccountId") long authAccountId);
    int insert(AccountExpiryHistoryRecord record);
}
