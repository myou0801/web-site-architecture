
# infrastructure モジュール設計  
（Record + toDomain/fromDomain 方式・Repository名は XxxRepositoryImpl）

## 0. 前提

- Java: 25
- Spring Framework: 6.2
- Spring Security: 6.5
- DB:
  - 本番: PostgreSQL
  - 開発: H2 (MODE=PostgreSQL)
- O/R マッパー: MyBatis
- Domain 層:
  - Entity / ValueObject / Repository interface / DomainService が `domain` モジュールに存在
- infrastructure の役割:
  - **DB と Domain の橋渡し**
  - MyBatis Mapper / `*Record` / Repository 実装

方針：

> MyBatis は「DB 行を表す Record 型（`*Record`）」だけ扱う。  
> Domain との変換は **Record 自身の `toDomain` / `fromDomain`** で行う。  
> Repository 実装クラス名は **`XxxRepositoryImpl`（インタフェース名 + Impl）** とする。

---

## 1. パッケージ構成

`ecsite/infrastructure/src/main/java`

```text
com.myou.ec.ecsite.infrastructure
 ├─ config
 │   ├─ DataSourceConfig          … DataSource / Tx 設定（PostgreSQL / H2）
 │   └─ MyBatisConfig             … SqlSessionFactory / MapperScan
 │
 └─ auth
     ├─ record                    … DB 行を表す Record 型（Domain ↔ Record 変換メソッド持ち）
     │   ├─ AuthUserRecord
     │   ├─ AuthRoleRecord
     │   ├─ AuthLoginHistoryRecord
     │   ├─ AuthAccountLockHistoryRecord
     │   └─ AuthPasswordHistoryRecord
     │
     ├─ mapper                    … MyBatis Mapper インタフェース
     │   ├─ AuthUserMapper
     │   ├─ AuthRoleMapper
     │   ├─ AuthLoginHistoryMapper
     │   ├─ AuthAccountLockHistoryMapper
     │   └─ AuthPasswordHistoryMapper
     │
     └─ repository                … domain Repository 実装（インタフェース + Impl）
         ├─ AuthUserRepositoryImpl
         ├─ AuthRoleRepositoryImpl
         ├─ AuthLoginHistoryRepositoryImpl
         ├─ AuthAccountLockHistoryRepositoryImpl
         └─ AuthPasswordHistoryRepositoryImpl
````

`src/main/resources`

```text
src/main/resources
 └─ mybatis
     └─ auth
         ├─ AuthUserMapper.xml
         ├─ AuthRoleMapper.xml
         ├─ AuthLoginHistoryMapper.xml
         ├─ AuthAccountLockHistoryMapper.xml
         └─ AuthPasswordHistoryMapper.xml
```

---

## 2. 共通パターン：Record + toDomain / fromDomain

### 2.1 ルール

* Record は **infrastructure** 側の型（domain からは参照しない）
* Record は **domain 型を知ってよい**（依存方向: infra → domain）
* 各 Record は以下のようなメソッドを持つ：

```java
public record XxxRecord(/* DBカラムに対応するフィールド */) {

    // DB → Domain
    public DomainXxx toDomain() { ... }

    // Domain → DB
    public static XxxRecord fromDomain(DomainXxx domain, /* now, operatedBy など */) { ... }
}
```

* Repository 実装（`XxxRepositoryImpl`）は

    * Mapper に `XxxRecord` を渡す／受け取る
    * `record.toDomain()` / `XxxRecord.fromDomain()` を呼ぶだけ

---

## 3. AUTH_USER の例

### 3.1 Record 定義（AuthUserRecord）

```java
package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.time.LocalDateTime;

/**
 * AUTH_USER テーブルの1行を表す Record。
 * Domain の AuthUser との変換を担当する。
 */
public record AuthUserRecord(
        long authUserId,
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

    // ===== DB → Domain =====
    public AuthUser toDomain() {
        return new AuthUser(
                new AuthUserId(authUserId),
                new LoginId(loginId),
                new EncodedPassword(loginPassword),
                enabled,
                deleted,
                createdAt,
                new LoginId(createdBy),
                updatedAt,
                new LoginId(updatedBy),
                versionNo
                // roleCodes は別 Repository で補う前提ならここでは空・別引数などで調整
        );
    }

    // ===== Domain → DB =====
    public static AuthUserRecord fromDomain(AuthUser user, LocalDateTime now) {
        long id = user.id() != null ? user.id().value() : 0L;
        String loginId = user.loginId().value();
        String encodedPassword = user.encodedPassword().value();

        return new AuthUserRecord(
                id,
                loginId,
                encodedPassword,
                user.enabled(),
                user.deleted(),
                user.createdAt() != null ? user.createdAt() : now,
                user.createdByLoginId() != null ? user.createdByLoginId().value() : loginId,
                now,
                user.updatedByLoginId() != null ? user.updatedByLoginId().value() : loginId,
                user.versionNo()
        );
    }
}
```

---

### 3.2 Mapper インタフェース（AuthUserMapper）

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

---

### 3.3 Mapper XML（AuthUserMapper.xml）

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.myou.ec.ecsite.infrastructure.auth.mapper.AuthUserMapper">

    <resultMap id="AuthUserRecordMap"
               type="com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord">

        <result column="AUTH_USER_ID"    property="authUserId" />
        <result column="LOGIN_ID"        property="loginId" />
        <result column="LOGIN_PASSWORD"  property="loginPassword" />

        <!-- CHAR(1) → boolean は BooleanTypeHandler 等で吸収 -->
        <result column="ENABLED_FLG"     property="enabled"
                javaType="boolean"
                typeHandler="org.apache.ibatis.type.BooleanTypeHandler"/>
        <result column="DELETED_FLG"     property="deleted"
                javaType="boolean"
                typeHandler="org.apache.ibatis.type.BooleanTypeHandler"/>

        <result column="CREATED_AT"      property="createdAt" />
        <result column="CREATED_BY"      property="createdBy" />
        <result column="UPDATED_AT"      property="updatedAt" />
        <result column="UPDATED_BY"      property="updatedBy" />
        <result column="VERSION_NO"      property="versionNo" />
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
          AND DELETED_FLG = '0'
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
          AND DELETED_FLG = '0'
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
            #{enabled,  typeHandler=org.apache.ibatis.type.BooleanTypeHandler},
            #{deleted,  typeHandler=org.apache.ibatis.type.BooleanTypeHandler},
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
            ENABLED_FLG    = #{enabled, typeHandler=org.apache.ibatis.type.BooleanTypeHandler},
            DELETED_FLG    = #{deleted, typeHandler=org.apache.ibatis.type.BooleanTypeHandler},
            UPDATED_AT     = #{updatedAt},
            UPDATED_BY     = #{updatedBy},
            VERSION_NO     = VERSION_NO + 1
        WHERE AUTH_USER_ID = #{authUserId}
          AND VERSION_NO   = #{versionNo}
    </update>
</mapper>
```

---

### 3.4 Repository 実装（AuthUserRepositoryImpl）

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthUserMapper;
import com.myou.ec.ecsite.infrastructure.auth.record.AuthUserRecord;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class AuthUserRepositoryImpl implements AuthUserRepository {

    private final AuthUserMapper mapper;
    private final Clock clock;

    public AuthUserRepositoryImpl(AuthUserMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public Optional<AuthUser> findById(AuthUserId authUserId) {
        AuthUserRecord record = mapper.findById(authUserId.value());
        return Optional.ofNullable(record).map(AuthUserRecord::toDomain);
    }

    @Override
    public Optional<AuthUser> findByLoginId(LoginId loginId) {
        AuthUserRecord record = mapper.findByLoginId(loginId.value());
        return Optional.ofNullable(record).map(AuthUserRecord::toDomain);
    }

    @Override
    public AuthUserId nextId() {
        // IDENTITY戦略なら通常使わない
        throw new UnsupportedOperationException("IDENTITY strategy: not supported");
    }

    @Override
    public void save(AuthUser authUser) {
        LocalDateTime now = LocalDateTime.now(clock);
        AuthUserRecord record = AuthUserRecord.fromDomain(authUser, now);
        if (authUser.id() == null) {
            mapper.insert(record);
            // 必要に応じて再読込して Domain 側に ID を反映する
        } else {
            mapper.update(record);
        }
    }
}
```

---

## 4. 他テーブルへの適用パターン

### 4.1 AUTH_LOGIN_HISTORY

#### Record

```java
package com.myou.ec.ecsite.infrastructure.auth.record;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginResult;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.time.LocalDateTime;

public record AuthLoginHistoryRecord(
        long authLoginHistoryId,
        long authUserId,
        LocalDateTime loginAt,
        String result,         // "SUCCESS" / "FAIL" / "LOCKED" / "DISABLED"
        String clientIp,
        String userAgent,
        LocalDateTime createdAt,
        String createdBy
) {

    public LoginHistory toDomain() {
        return new LoginHistory(
                new AuthUserId(authUserId),
                LoginResult.valueOf(result),
                loginAt,
                clientIp,
                userAgent,
                new LoginId(createdBy),
                createdAt
        );
    }

    public static AuthLoginHistoryRecord fromDomain(LoginHistory history, LocalDateTime now) {
        return new AuthLoginHistoryRecord(
                0L,
                history.authUserId().value(),
                history.loginAt(),
                history.result().name(),
                history.clientIp(),
                history.userAgent(),
                now,
                history.createdBy().value()
        );
    }
}
```

#### Mapper

```java
@Mapper
public interface AuthLoginHistoryMapper {

    void insert(AuthLoginHistoryRecord record);

    List<AuthLoginHistoryRecord> findRecentByUserId(@Param("authUserId") long authUserId,
                                                    @Param("limit") int limit);
}
```

#### Repository 実装（AuthLoginHistoryRepositoryImpl）

```java
@Repository
public class AuthLoginHistoryRepositoryImpl implements AuthLoginHistoryRepository {

    private final AuthLoginHistoryMapper mapper;
    private final Clock clock;

    public AuthLoginHistoryRepositoryImpl(AuthLoginHistoryMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void save(LoginHistory history) {
        LocalDateTime now = LocalDateTime.now(clock);
        AuthLoginHistoryRecord record = AuthLoginHistoryRecord.fromDomain(history, now);
        mapper.insert(record);
    }

    @Override
    public List<LoginHistory> findRecentByUserId(AuthUserId authUserId, int limit) {
        return mapper.findRecentByUserId(authUserId.value(), limit).stream()
                .map(AuthLoginHistoryRecord::toDomain)
                .toList();
    }
}
```

---

### 4.2 AUTH_ACCOUNT_LOCK_HISTORY

同様に：

* `AuthAccountLockHistoryRecord` に `toDomain` / `fromDomain`
* `AuthAccountLockHistoryMapper`
* `AuthAccountLockHistoryRepositoryImpl` （domain の `AuthAccountLockHistoryRepository` を実装）

---

### 4.3 AUTH_PASSWORD_HISTORY

同様に：

* `AuthPasswordHistoryRecord` に `toDomain` / `fromDomain`
* `AuthPasswordHistoryMapper`
* `AuthPasswordHistoryRepositoryImpl`

---

## 5. まとめ

* **MyBatis** は `*Record`（DB 用の record 型）＋プリミティブだけ扱う。
* 各 `*Record` は

    * `toDomain()`（DB → Domain）
    * `static fromDomain(Domain, now, ...)`（Domain → DB）
      を持ち、変換ロジックを自分の中に閉じ込める。
* Repository 実装クラスは、インタフェース名 + `Impl` とし、

    * `AuthUserRepositoryImpl`
    * `AuthLoginHistoryRepositoryImpl`
      などの名前で `@Repository` として定義する。
* Repository の責務は

    * Mapper 呼び出し
    * Record の変換メソッド呼び出し
      程度にスリム化され、Domain との境界も明確になる。

この形で進めれば、
**「Domain は純粋」「infra は Record + Mapper + XxxRepositoryImpl」** というきれいな構成で認証基盤を組み立てられると思います。


