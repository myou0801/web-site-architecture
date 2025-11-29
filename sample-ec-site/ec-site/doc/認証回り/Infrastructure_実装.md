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
 │   ├─ AuthAccountRecord
 │   ├─ AuthRoleRecord
 │   ├─ AuthLoginHistoryRecord
 │   ├─ AuthPasswordHistoryRecord
 │   └─ AuthAccountLockHistoryRecord
 │
 ├─ mapper
 │   ├─ AuthAccountMapper
 │   ├─ AuthRoleMapper
 │   ├─ AuthLoginHistoryMapper
 │   ├─ AuthPasswordHistoryMapper
 │   └─ AuthAccountLockHistoryMapper
 │
 └─ repository
     ├─ AuthAccountRepositoryImpl
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

### AuthAccountRecord.java

```java
package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AUTH_ACCOUNT テーブルの1行を表す Record。
 */
public record AuthAccountRecord(
        Long authAccountId,
        String userId,
        String loginPassword,
        boolean enabled,
        boolean deleted,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        long versionNo
) {

    public AuthAccount toDomain(List<RoleCode> roleCodes) {
        return new AuthAccount(
                authAccountId != null ? new AuthAccountId(authAccountId) : null,
                new UserId(userId),
                new EncodedPassword(loginPassword),
                enabled,
                deleted,
                roleCodes,
                createdAt,
                new UserId(createdBy),
                updatedAt,
                new UserId(updatedBy),
                versionNo
        );
    }

    public static AuthAccountRecord fromDomain(AuthAccount user) {
        Long id = user.id() != null ? user.id().value() : null;
        return new AuthAccountRecord(
                id,
                user.userId().value(),
                user.encodedPassword().value(),
                user.enabled(),
                user.deleted(),
                user.createdAt(),
                user.createdByUserId().value(),
                user.updatedAt(),
                user.updatedByUserId().value(),
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
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;

import java.time.LocalDateTime;

public record AuthLoginHistoryRecord(
        Long authLoginHistoryId,
        long authAccountId,
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
                new AuthAccountId(authAccountId),
                loginAt,
                LoginResult.valueOf(result),
                clientIp,
                userAgent,
                createdAt,
                new UserId(createdBy)
        );
    }

    public static AuthLoginHistoryRecord fromDomain(LoginHistory history) {
        return new AuthLoginHistoryRecord(
                history.id(),
                history.authAccountId().value(),
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
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordChangeType;

import java.time.LocalDateTime;

public record AuthPasswordHistoryRecord(
        Long authPasswordHistoryId,
        long authAccountId,
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
                new AuthAccountId(authAccountId),
                new EncodedPassword(loginPassword),
                PasswordChangeType.valueOf(changeType),
                changedAt,
                new UserId(changedBy),
                createdAt,
                new UserId(createdBy)
        );
    }

    public static AuthPasswordHistoryRecord fromDomain(PasswordHistory history) {
        return new AuthPasswordHistoryRecord(
                history.id(),
                history.authAccountId().value(),
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
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;

public record AuthAccountLockHistoryRecord(
        Long authAccountLockHistoryId,
        long authAccountId,
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
                new AuthAccountId(authAccountId),
                locked,
                occurredAt,
                reason,
                new UserId(operatedBy),
                createdAt,
                new UserId(createdBy)
        );
    }

    public static AuthAccountLockHistoryRecord fromDomain(AccountLockEvent event) {
        return new AuthAccountLockHistoryRecord(
                event.id(),
                event.authAccountId().value(),
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

### AuthAccountMapper.java

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthAccountMapper {

    AuthAccountRecord findById(@Param("authAccountId") long authAccountId);

    AuthAccountRecord findByUserId(@Param("userId") String userId);

    void insert(AuthAccountRecord record);

    void update(AuthAccountRecord record);
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

    List<String> findRoleCodesByAccountId(@Param("authAccountId") long authAccountId);

    void deleteAccountRoles(@Param("authAccountId") long authAccountId);

    void insertAccountRole(@Param("authAccountId") long authAccountId,
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

    List<AuthLoginHistoryRecord> findRecentByAccountId(@Param("authAccountId") long authAccountId,
                                                    @Param("limit") int limit);

    LocalDateTime findPreviousSuccessLoginAtByAccountId(@Param("authAccountId") long authAccountId);

    Integer countConsecutiveFailuresSinceLastSuccessOrUnlockByAccountId(@Param("authAccountId") long authAccountId);
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

    List<AuthPasswordHistoryRecord> findRecentByAccountId(@Param("authAccountId") long authAccountId,
                                                       @Param("limit") int limit);

    AuthPasswordHistoryRecord findLastByAccountId(@Param("authAccountId") long authAccountId);
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

    AuthAccountLockHistoryRecord findLatestByAccountId(@Param("authAccountId") long authAccountId);
}
```

---

## repository パッケージ（実装クラス）

### AuthAccountRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;
import com.myou.ec.ecsite.domain.auth.repository.AuthRoleRepository;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuthAccountRepositoryImpl implements AuthAccountRepository {

    private final AuthAccountMapper userMapper;
    private final AuthRoleRepository authRoleRepository;

    public AuthAccountRepositoryImpl(AuthAccountMapper userMapper,
                                  AuthRoleRepository authRoleRepository) {
        this.userMapper = userMapper;
        this.authRoleRepository = authRoleRepository;
    }

    @Override
    public Optional<AuthAccount> findById(AuthAccountId id) {
        AuthAccountRecord record = userMapper.findById(id.value());
        if (record == null) {
            return Optional.empty();
        }
        List<RoleCode> roles = authRoleRepository.findRoleCodesByAccountId(id);
        return Optional.of(record.toDomain(roles));
    }

    @Override
    public Optional<AuthAccount> findByUserId(UserId userId) {
        AuthAccountRecord record = userMapper.findByUserId(userId.value());
        if (record == null) {
            return Optional.empty();
        }
        AuthAccountId accountId = new AuthAccountId(record.authAccountId());
        List<RoleCode> roles = authRoleRepository.findRoleCodesByAccountId(accountId);
        return Optional.of(record.toDomain(roles));
    }

    @Override
    public void save(AuthAccount user) {
        AuthAccountRecord record = AuthAccountRecord.fromDomain(user);
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
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
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
    public List<RoleCode> findRoleCodesByAccountId(AuthAccountId authAccountId) {
        return mapper.findRoleCodesByAccountId(authAccountId.value()).stream()
                .map(RoleCode::new)
                .toList();
    }

    @Override
    public void saveAccountRoles(AuthAccountId authAccountId, List<RoleCode> roleCodes) {
        long id = authAccountId.value();
        mapper.deleteAccountRoles(id);
        for (RoleCode roleCode : roleCodes) {
            mapper.insertAccountRole(id, roleCode.value());
        }
    }
}
```

### AuthLoginHistoryRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
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
    public List<LoginHistory> findRecentByAccountId(AuthAccountId accountId, int limit) {
        return mapper.findRecentByAccountId(accountId.value(), limit).stream()
                .map(AuthLoginHistoryRecord::toDomain)
                .toList();
    }

    @Override
    public Optional<LocalDateTime> findPreviousSuccessLoginAtByAccountId(AuthAccountId accountId) {
        return Optional.ofNullable(mapper.findPreviousSuccessLoginAtByAccountId(accountId.value()));
    }

    @Override
    public int countConsecutiveFailuresSinceLastSuccessOrUnlockByAccountId(AuthAccountId accountId) {
        Integer count = mapper.countConsecutiveFailuresSinceLastSuccessOrUnlockByAccountId(accountId.value());
        return count != null ? count : 0;
    }
}
```

### AuthPasswordHistoryRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
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
    public List<PasswordHistory> findRecentByAccountId(AuthAccountId accountId, int limit) {
        return mapper.findRecentByAccountId(accountId.value(), limit).stream()
                .map(AuthPasswordHistoryRecord::toDomain)
                .toList();
    }

    @Override
    public Optional<PasswordHistory> findLastByAccountId(AuthAccountId accountId) {
        AuthPasswordHistoryRecord record = mapper.findLastByAccountId(accountId.value());
        return Optional.ofNullable(record).map(AuthPasswordHistoryRecord::toDomain);
    }
}
```

### AuthAccountLockHistoryRepositoryImpl.java

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
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
    public Optional<AccountLockEvent> findLatestByAccountId(AuthAccountId accountId) {
        AuthAccountLockHistoryRecord record = mapper.findLatestByAccountId(accountId.value());
        return Optional.ofNullable(record).map(AuthAccountLockHistoryRecord::toDomain);
    }

    @Override
    public LockStatus getLockStatusByAccountId(AuthAccountId accountId) {
        return findLatestByAccountId(accountId)
                .map(e -> e.locked() ? LockStatus.LOCKED : LockStatus.UNLOCKED)
                .orElse(LockStatus.UNLOCKED);
    }
}
```

---

## MyBatis XML（例）

`src/main/resources/mybatis/auth/AuthAccountMapper.xml` だけフルで書きます。他はこのパターンで調整すればOKです。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper">

    <resultMap id="AuthAccountRecordMap"
               type="com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord">

        <id     column="AUTH_ACCOUNT_ID"   property="authAccountId"/>
        <result column="USER_ID"       property="userId"/>
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

    <select id="findById" resultMap="AuthAccountRecordMap">
        SELECT
            AUTH_ACCOUNT_ID,
            USER_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            DELETED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        FROM AUTH_ACCOUNT
        WHERE AUTH_ACCOUNT_ID = #{authAccountId}
    </select>

    <select id="findByUserId" resultMap="AuthAccountRecordMap">
        SELECT
            AUTH_ACCOUNT_ID,
            USER_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            DELETED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        FROM AUTH_ACCOUNT
        WHERE USER_ID = #{userId}
    </select>

    <insert id="insert"
            parameterType="com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord"
            useGeneratedKeys="true"
            keyProperty="authAccountId">
        INSERT INTO AUTH_ACCOUNT (
            USER_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            DELETED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        ) VALUES (
            #{userId},
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
            parameterType="com.myou.ec.ecsite.infrastructure.auth.record.AuthAccountRecord">
        UPDATE AUTH_ACCOUNT
        SET
            USER_ID       = #{userId},
            LOGIN_PASSWORD = #{loginPassword},
            ENABLED_FLG    = #{enabled},
            DELETED_FLG    = #{deleted},
            UPDATED_AT     = #{updatedAt},
            UPDATED_BY     = #{updatedBy},
            VERSION_NO     = VERSION_NO + 1
        WHERE AUTH_ACCOUNT_ID = #{authAccountId}
          AND VERSION_NO   = #{versionNo}
    </update>

</mapper>
```

---

ここまでで、`auth` ドメインに対応する infrastructure 実装の「骨組み」は一通りそろいました。

* 実DBのカラム名・型（BOOLEAN か CHAR(1) か 等）
* スキーマ名（`public.AUTH_ACCOUNT` など）
* H2 用の DDL

は、実際の DDL に合わせて XML の細部を調整してもらえればOKです。

次のステップとしては、

* Spring 設定（`@MapperScan` / DataSource / SqlSessionFactory）
* `sharedService` / UseCase 実装からこの RepositoryImpl を叩く部分

あたりを詰めていく感じになりますが、そこまでやりたくなったらまた一緒に作っていきましょう。
