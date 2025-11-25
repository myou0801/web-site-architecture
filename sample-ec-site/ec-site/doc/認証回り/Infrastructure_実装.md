じゃあ infrastructure 側を一式、`auth` ドメイン用としてまとめて出しますね。
構成は：

* `record` パッケージ … DB行用の record 型（toDomain / fromDomain 持ち）
* `mapper` パッケージ … MyBatis Mapper インタフェース
* `repository` パッケージ … Domain の Repository 実装（XxxRepositoryImpl）
* `resources/mybatis/auth` … Mapper XML

コメントは最低限にしてあるので、そのままコピペして調整していけると思います。

---

## README（infrastructure/auth 用）


# ecsite-auth Infrastructure (MyBatis)

認証系ドメイン (`com.myou.ec.ecsite.domain.auth`) のための infrastructure 実装です。

## パッケージ構成

```text
com.myou.ec.ecsite.infrastructure.auth
 ├─ record
 │   ├─ AuthUserRecord
 │   ├─ AuthRoleRecord
 │   ├─ AuthLoginHistoryRecord
 │   ├─ AuthPasswordHistoryRecord
 │   └─ AuthAccountLockHistoryRecord
 │
 ├─ mapper
 │   ├─ AuthUserMapper
 │   ├─ AuthRoleMapper
 │   ├─ AuthLoginHistoryMapper
 │   ├─ AuthPasswordHistoryMapper
 │   └─ AuthAccountLockHistoryMapper
 │
 └─ repository
     ├─ AuthUserRepositoryImpl
     ├─ AuthRoleRepositoryImpl
     ├─ AuthLoginHistoryRepositoryImpl
     ├─ AuthPasswordHistoryRepositoryImpl
     └─ AuthAccountLockHistoryRepositoryImpl
````

* `record` は DB テーブル1行分を表す record 型で、Domain との変換

    * `toDomain(...)`
    * `static fromDomain(...)`
* Repository 実装は Domain の Repository インタフェースを実装し、

    * Mapper に `*Record` を渡す
    * `*Record` から Domain へ変換する

MyBatis の XML は `src/main/resources/mybatis/auth` 以下に配置します。


---

## record パッケージ

### AuthUserRecord.java

```java
package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AUTH_USER テーブルの1行を表す Record。
 */
public record AuthUserRecord(
        Long authUserId,
        String loginId,
        String loginPassword,
        boolean enabled,
        boolean deleted,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        long versionNo
) {

    public AuthUser toDomain(List<RoleCode> roleCodes) {
        return new AuthUser(
                authUserId != null ? new AuthUserId(authUserId) : null,
                new LoginId(loginId),
                new EncodedPassword(loginPassword),
                enabled,
                deleted,
                roleCodes,
                createdAt,
                new LoginId(createdBy),
                updatedAt,
                new LoginId(updatedBy),
                versionNo
        );
    }

    public static AuthUserRecord fromDomain(AuthUser user) {
        Long id = user.id() != null ? user.id().value() : null;
        return new AuthUserRecord(
                id,
                user.loginId().value(),
                user.encodedPassword().value(),
                user.enabled(),
                user.deleted(),
                user.createdAt(),
                user.createdByLoginId().value(),
                user.updatedAt(),
                user.updatedByLoginId().value(),
                user.versionNo()
        );
    }
}
````

### AuthRoleRecord.java

```java
package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthRole;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;

public record AuthRoleRecord(
        String roleCode,
        String roleName,
        String description,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {

    public AuthRole toDomain() {
        return new AuthRole(
                new RoleCode(roleCode),
                roleName,
                description
        );
    }
}
```

### AuthLoginHistoryRecord.java

```java
package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;

public record AuthLoginHistoryRecord(
        Long authLoginHistoryId,
        long authUserId,
        LocalDateTime loginAt,
        String result,
        String clientIp,
        String userAgent,
        LocalDateTime createdAt,
        String createdBy
) {

    public LoginHistory toDomain() {
        return new LoginHistory(
                authLoginHistoryId,
                new AuthUserId(authUserId),
                loginAt,
                LoginResult.valueOf(result),
                clientIp,
                userAgent,
                createdAt,
                new LoginId(createdBy)
        );
    }

    public static AuthLoginHistoryRecord fromDomain(LoginHistory history) {
        return new AuthLoginHistoryRecord(
                history.id(),
                history.authUserId().value(),
                history.loginAt(),
                history.result().name(),
                history.clientIp(),
                history.userAgent(),
                history.createdAt(),
                history.createdBy().value()
        );
    }
}
```

### AuthPasswordHistoryRecord.java

```java
package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;

import java.time.LocalDateTime;

public record AuthPasswordHistoryRecord(
        Long authPasswordHistoryId,
        long authUserId,
        String loginPassword,
        String changeType,
        LocalDateTime changedAt,
        String changedBy,
        LocalDateTime createdAt,
        String createdBy
) {

    public PasswordHistory toDomain() {
        return new PasswordHistory(
                authPasswordHistoryId,
                new AuthUserId(authUserId),
                new EncodedPassword(loginPassword),
                PasswordChangeType.valueOf(changeType),
                changedAt,
                new LoginId(changedBy),
                createdAt,
                new LoginId(createdBy)
        );
    }

    public static AuthPasswordHistoryRecord fromDomain(PasswordHistory history) {
        return new AuthPasswordHistoryRecord(
                history.id(),
                history.authUserId().value(),
                history.encodedPassword().value(),
                history.changeType().name(),
                history.changedAt(),
                history.changedBy().value(),
                history.createdAt(),
                history.createdBy().value()
        );
    }
}
```

### AuthAccountLockHistoryRecord.java

```java
package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.time.LocalDateTime;

public record AuthAccountLockHistoryRecord(
        Long authAccountLockHistoryId,
        long authUserId,
        boolean locked,
        LocalDateTime occurredAt,
        String reason,
        String operatedBy,
        LocalDateTime createdAt,
        String createdBy
) {

    public AccountLockEvent toDomain() {
        return new AccountLockEvent(
                authAccountLockHistoryId,
                new AuthUserId(authUserId),
                locked,
                occurredAt,
                reason,
                new LoginId(operatedBy),
                createdAt,
                new LoginId(createdBy)
        );
    }

    public static AuthAccountLockHistoryRecord fromDomain(AccountLockEvent event) {
        return new AuthAccountLockHistoryRecord(
                event.id(),
                event.authUserId().value(),
                event.locked(),
                event.occurredAt(),
                event.reason(),
                event.operatedBy().value(),
                event.createdAt(),
                event.createdBy().value()
        );
    }
}
```

---

## mapper パッケージ（Java インタフェース）

### AuthUserMapper.java

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthUserMapper {

    AuthUserRecord findById(@Param("authUserId") long authUserId);

    AuthUserRecord findByLoginId(@Param("loginId") String loginId);

    void insert(AuthUserRecord record);

    void update(AuthUserRecord record);
}
```

### AuthRoleMapper.java

```java
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
```

### AuthLoginHistoryMapper.java

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthLoginHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuthLoginHistoryMapper {

    void insert(AuthLoginHistoryRecord record);

    List<AuthLoginHistoryRecord> findRecentByUserId(@Param("authUserId") long authUserId,
                                                    @Param("limit") int limit);

    LocalDateTime findPreviousSuccessLoginAt(@Param("authUserId") long authUserId);

    Integer countConsecutiveFailuresSinceLastSuccessOrUnlock(@Param("authUserId") long authUserId);
}
```

### AuthPasswordHistoryMapper.java

```java
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
```

### AuthAccountLockHistoryMapper.java

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthAccountLockHistoryMapper {

    void insert(AuthAccountLockHistoryRecord record);

    AuthAccountLockHistoryRecord findLatestByUserId(@Param("authUserId") long authUserId);
}
```

---

## repository パッケージ（実装クラス）

### AuthUserRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthUserMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthUserRepositoryImpl implements AuthUserRepository {

    private final AuthUserMapper userMapper;
    private final AuthRoleRepository authRoleRepository;

    public AuthUserRepositoryImpl(AuthUserMapper userMapper,
                                  AuthRoleRepository authRoleRepository) {
        this.userMapper = userMapper;
        this.authRoleRepository = authRoleRepository;
    }

    @Override
    public Optional<AuthUser> findById(AuthUserId id) {
        AuthUserRecord record = userMapper.findById(id.value());
        if (record == null) {
            return Optional.empty();
        }
        List<RoleCode> roles = authRoleRepository.findRoleCodesByUserId(id);
        return Optional.of(record.toDomain(roles));
    }

    @Override
    public Optional<AuthUser> findByLoginId(LoginId loginId) {
        AuthUserRecord record = userMapper.findByLoginId(loginId.value());
        if (record == null) {
            return Optional.empty();
        }
        AuthUserId userId = new AuthUserId(record.authUserId());
        List<RoleCode> roles = authRoleRepository.findRoleCodesByUserId(userId);
        return Optional.of(record.toDomain(roles));
    }

    @Override
    public void save(AuthUser user) {
        AuthUserRecord record = AuthUserRecord.fromDomain(user);
        if (user.id() == null) {
            userMapper.insert(record);
        } else {
            userMapper.update(record);
        }
    }
}
```

### AuthRoleRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthRole;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthRoleMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthRoleRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuthRoleRepositoryImpl implements AuthRoleRepository {

    private final AuthRoleMapper mapper;

    public AuthRoleRepositoryImpl(AuthRoleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AuthRole> findAll() {
        return mapper.findAll().stream()
                .map(AuthRoleRecord::toDomain)
                .toList();
    }

    @Override
    public List<RoleCode> findRoleCodesByUserId(AuthUserId authUserId) {
        return mapper.findRoleCodesByUserId(authUserId.value()).stream()
                .map(RoleCode::new)
                .toList();
    }

    @Override
    public void saveUserRoles(AuthUserId authUserId, List<RoleCode> roleCodes) {
        long id = authUserId.value();
        mapper.deleteUserRoles(id);
        for (RoleCode roleCode : roleCodes) {
            mapper.insertUserRole(id, roleCode.value());
        }
    }
}
```

### AuthLoginHistoryRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthLoginHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthLoginHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthLoginHistoryRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthLoginHistoryRepositoryImpl implements AuthLoginHistoryRepository {

    private final AuthLoginHistoryMapper mapper;

    public AuthLoginHistoryRepositoryImpl(AuthLoginHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(LoginHistory history) {
        AuthLoginHistoryRecord record = AuthLoginHistoryRecord.fromDomain(history);
        mapper.insert(record);
    }

    @Override
    public List<LoginHistory> findRecentByUserId(AuthUserId userId, int limit) {
        return mapper.findRecentByUserId(userId.value(), limit).stream()
                .map(AuthLoginHistoryRecord::toDomain)
                .toList();
    }

    @Override
    public Optional<LocalDateTime> findPreviousSuccessLoginAt(AuthUserId userId) {
        return Optional.ofNullable(mapper.findPreviousSuccessLoginAt(userId.value()));
    }

    @Override
    public int countConsecutiveFailuresSinceLastSuccessOrUnlock(AuthUserId userId) {
        Integer count = mapper.countConsecutiveFailuresSinceLastSuccessOrUnlock(userId.value());
        return count != null ? count : 0;
    }
}
```

### AuthPasswordHistoryRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthPasswordHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthPasswordHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthPasswordHistoryRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthPasswordHistoryRepositoryImpl implements AuthPasswordHistoryRepository {

    private final AuthPasswordHistoryMapper mapper;

    public AuthPasswordHistoryRepositoryImpl(AuthPasswordHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(PasswordHistory history) {
        AuthPasswordHistoryRecord record = AuthPasswordHistoryRecord.fromDomain(history);
        mapper.insert(record);
    }

    @Override
    public List<PasswordHistory> findRecentByUserId(AuthUserId userId, int limit) {
        return mapper.findRecentByUserId(userId.value(), limit).stream()
                .map(AuthPasswordHistoryRecord::toDomain)
                .toList();
    }

    @Override
    public Optional<PasswordHistory> findLastByUserId(AuthUserId userId) {
        AuthPasswordHistoryRecord record = mapper.findLastByUserId(userId.value());
        return Optional.ofNullable(record).map(AuthPasswordHistoryRecord::toDomain);
    }
}
```

### AuthAccountLockHistoryRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LockStatus;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountLockHistoryRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountLockHistoryMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountLockHistoryRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AuthAccountLockHistoryRepositoryImpl implements AuthAccountLockHistoryRepository {

    private final AuthAccountLockHistoryMapper mapper;

    public AuthAccountLockHistoryRepositoryImpl(AuthAccountLockHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(AccountLockEvent event) {
        AuthAccountLockHistoryRecord record = AuthAccountLockHistoryRecord.fromDomain(event);
        mapper.insert(record);
    }

    @Override
    public Optional<AccountLockEvent> findLatestByUserId(AuthUserId userId) {
        AuthAccountLockHistoryRecord record = mapper.findLatestByUserId(userId.value());
        return Optional.ofNullable(record).map(AuthAccountLockHistoryRecord::toDomain);
    }

    @Override
    public LockStatus getLockStatus(AuthUserId userId) {
        return findLatestByUserId(userId)
                .map(e -> e.locked() ? LockStatus.LOCKED : LockStatus.UNLOCKED)
                .orElse(LockStatus.UNLOCKED);
    }
}
```

---

## MyBatis XML（例）

`src/main/resources/mybatis/auth/AuthUserMapper.xml` だけフルで書きます。他はこのパターンで調整すればOKです。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.myou.ec.ecsite.infrastructure.auth.mapper.AuthUserMapper">

    <resultMap id="AuthUserRecordMap"
               type="com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord">

        <id     column="AUTH_USER_ID"   property="authUserId"/>
        <result column="LOGIN_ID"       property="loginId"/>
        <result column="LOGIN_PASSWORD" property="loginPassword"/>

        <!-- DB側を BOOLEAN にしておく想定。CHAR(1) なら BooleanTypeHandler などを利用 -->
        <result column="ENABLED_FLG"    property="enabled"/>
        <result column="DELETED_FLG"    property="deleted"/>

        <result column="CREATED_AT"     property="createdAt"/>
        <result column="CREATED_BY"     property="createdBy"/>
        <result column="UPDATED_AT"     property="updatedAt"/>
        <result column="UPDATED_BY"     property="updatedBy"/>
        <result column="VERSION_NO"     property="versionNo"/>
    </resultMap>

    <select id="findById" resultMap="AuthUserRecordMap">
        SELECT
            AUTH_USER_ID,
            LOGIN_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            DELETED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        FROM AUTH_USER
        WHERE AUTH_USER_ID = #{authUserId}
    </select>

    <select id="findByLoginId" resultMap="AuthUserRecordMap">
        SELECT
            AUTH_USER_ID,
            LOGIN_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            DELETED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        FROM AUTH_USER
        WHERE LOGIN_ID = #{loginId}
    </select>

    <insert id="insert"
            parameterType="com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord"
            useGeneratedKeys="true"
            keyProperty="authUserId">
        INSERT INTO AUTH_USER (
            LOGIN_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            DELETED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        ) VALUES (
            #{loginId},
            #{loginPassword},
            #{enabled},
            #{deleted},
            #{createdAt},
            #{createdBy},
            #{updatedAt},
            #{updatedBy},
            #{versionNo}
        )
    </insert>

    <update id="update"
            parameterType="com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord">
        UPDATE AUTH_USER
        SET
            LOGIN_ID       = #{loginId},
            LOGIN_PASSWORD = #{loginPassword},
            ENABLED_FLG    = #{enabled},
            DELETED_FLG    = #{deleted},
            UPDATED_AT     = #{updatedAt},
            UPDATED_BY     = #{updatedBy},
            VERSION_NO     = VERSION_NO + 1
        WHERE AUTH_USER_ID = #{authUserId}
          AND VERSION_NO   = #{versionNo}
    </update>

</mapper>
```

---

ここまでで、`auth` ドメインに対応する infrastructure 実装の「骨組み」は一通りそろいました。

* 実DBのカラム名・型（BOOLEAN か CHAR(1) か 等）
* スキーマ名（`public.AUTH_USER` など）
* H2 用の DDL

は、実際の DDL に合わせて XML の細部を調整してもらえればOKです。

次のステップとしては、

* Spring 設定（`@MapperScan` / DataSource / SqlSessionFactory）
* `sharedService` / UseCase 実装からこの RepositoryImpl を叩く部分

あたりを詰めていく感じになりますが、そこまでやりたくなったらまた一緒に作っていきましょう。
