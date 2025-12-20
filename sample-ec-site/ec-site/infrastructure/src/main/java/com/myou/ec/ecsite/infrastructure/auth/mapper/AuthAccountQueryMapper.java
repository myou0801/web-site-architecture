package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.application.auth.dto.AuthAccountSearchParam;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountDetailRecord;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountSummaryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AuthAccountQueryMapper {

    List<AuthAccountSummaryRecord> selectSummaries(AuthAccountSearchParam param);

    long countSummaries(AuthAccountSearchParam param);

    Optional<AuthAccountDetailRecord> selectDetailByUserId(@Param("userId") String userId);
}
