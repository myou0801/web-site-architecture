package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthRoleRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthRoleMapper {

    List<AuthRoleRecord> findAll();

    List<String> findRoleCodesByUserId(@Param("authUserId") long authUserId);

    void deleteUserRoles(@Param("authUserId") long authUserId);

    void insertUserRole(@Param("authUserId") long authUserId,
                        @Param("roleCode") String roleCode);
}
