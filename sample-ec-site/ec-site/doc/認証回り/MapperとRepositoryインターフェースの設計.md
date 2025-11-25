じゃあここからは、**MyBatis Mapper と Repository インタフェースの「設計」**をまとめていきますね。
細かい SQL も含めて、まずは **AUTH_USER 周りをフルで例示**して、残りはパターンだけ押さえる形にします。

---

# 1. 基本方針

### 1-1. レイヤとパッケージ

* **Domain（インタフェース）**

    * `com.myou.ec.ecsite.domain.auth.repository.*`
    * DDD の Repository インタフェース（ValueObject / Entity を引数・戻り値に使う）

* **Infrastructure（MyBatis実装）**

    * Mapper インタフェース

        * `com.myou.ec.ecsite.infrastructure.auth.mapper.*`
    * Repository 実装クラス

        * `com.myou.ec.ecsite.infrastructure.auth.repository.*`
    * Mapper XML

        * `classpath*:mybatis/auth/*.xml`（例）

依存関係：

```text
domain.auth.repository  ←(implements)  infrastructure.auth.repository
                             ↑
                 infrastructure.auth.mapper + XML（MyBatis）
```

---

# 2. AUTH_USER：詳細なパターン

## 2-1. Domain 側 Repository インタフェース

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;

import java.util.Optional;

public interface AuthUserRepository {

    Optional<AuthUser> findById(AuthUserId authUserId);

    Optional<AuthUser> findByLoginId(LoginId loginId);

    AuthUserId nextId();                 // 採番（IDENTITY を使うなら不要でもOK）

    void save(AuthUser authUser);        // insert or update を内部で判定
}
```

* Domain 層からは **VO / Entity だけ見える**。
* `save` 内で「新規か更新か」は `authUser.id()` の有無等で判定。

---

## 2-2. Infrastructure 側 Mapper インタフェース（MyBatis）

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthUserMapper {

    AuthUser findById(@Param("authUserId") long authUserId);

    AuthUser findByLoginId(@Param("loginId") String loginId);

    void insert(AuthUser authUser);

    void update(AuthUser authUser);
}
```

* MyBatis の Mapper は **インフラ側で domain の AuthUser を直接扱う**想定。
* VO（`AuthUserId`, `LoginId`, `EncodedPassword` など）は
  TypeHandler で DB と変換するか、`AuthUserMapper` 側で `getValue()` を呼ぶかのどちらか。

ここでは **XML で constructor + TypeHandler を使う方式**を想定します。

---

## 2-3. AuthUserMapper.xml の例

`src/main/resources/mybatis/auth/AuthUserMapper.xml` など。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.myou.ec.ecsite.infrastructure.auth.mapper.AuthUserMapper">

    <!-- ValueObject / boolean 用の TypeHandler を使う前提例
         - AuthUserIdTypeHandler      : BIGINT ↔ AuthUserId
         - LoginIdTypeHandler         : VARCHAR ↔ LoginId
         - EncodedPasswordTypeHandler : VARCHAR ↔ EncodedPassword
         - BooleanFlagTypeHandler     : CHAR('1'/'0') ↔ boolean
    -->

    <resultMap id="AuthUserResultMap" type="com.myou.ec.ecsite.domain.auth.model.AuthUser">

        <constructor>
            <arg column="AUTH_USER_ID"
                 javaType="com.myou.ec.ecsite.domain.auth.model.value.AuthUserId"
                 typeHandler="com.myou.ec.ecsite.infrastructure.mybatis.type.AuthUserIdTypeHandler"/>

            <arg column="LOGIN_ID"
                 javaType="com.myou.ec.ecsite.domain.auth.model.value.LoginId"
                 typeHandler="com.myou.ec.ecsite.infrastructure.mybatis.type.LoginIdTypeHandler"/>

            <arg column="LOGIN_PASSWORD"
                 javaType="com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword"
                 typeHandler="com.myou.ec.ecsite.infrastructure.mybatis.type.EncodedPasswordTypeHandler"/>

            <arg column="ENABLED_FLG"
                 javaType="boolean"
                 typeHandler="com.myou.ec.ecsite.infrastructure.mybatis.type.BooleanFlagTypeHandler"/>

            <!-- roleCodes(List<RoleCode>) は別クエリで取得する前提とし、
                 コンストラクタ引数には渡さないパターンでもOK。
                 ここでは簡略化のため、空リストで構築して後から DomainService/Repository で補完してもよい。 -->
            <arg column="DUMMY_ROLE_CODES"
                 javaType="java.util.List"
                 select="selectEmptyRoleCodes"/>  <!-- ダミー or default コンストラクタを追加しても良い -->
        </constructor>

    </resultMap>

    <select id="findById" resultMap="AuthUserResultMap">
        SELECT
            AUTH_USER_ID,
            LOGIN_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        FROM AUTH_USER
        WHERE AUTH_USER_ID = #{authUserId}
          AND DELETED_FLG = '0'
    </select>

    <select id="findByLoginId" resultMap="AuthUserResultMap">
        SELECT
            AUTH_USER_ID,
            LOGIN_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        FROM AUTH_USER
        WHERE LOGIN_ID = #{loginId}
          AND DELETED_FLG = '0'
    </select>

    <insert id="insert" parameterType="com.myou.ec.ecsite.domain.auth.model.AuthUser"
            useGeneratedKeys="true" keyProperty="id.value">
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
            #{loginId,  typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.LoginIdTypeHandler},
            #{encodedPassword, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.EncodedPasswordTypeHandler},
            #{enabled,  typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.BooleanFlagTypeHandler},
            '0',
            CURRENT_TIMESTAMP,
            #{createdByLoginId},
            CURRENT_TIMESTAMP,
            #{createdByLoginId},
            0
        )
    </insert>

    <update id="update" parameterType="com.myou.ec.ecsite.domain.auth.model.AuthUser">
        UPDATE AUTH_USER
        SET
            LOGIN_ID       = #{loginId, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.LoginIdTypeHandler},
            LOGIN_PASSWORD = #{encodedPassword, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.EncodedPasswordTypeHandler},
            ENABLED_FLG    = #{enabled, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.BooleanFlagTypeHandler},
            UPDATED_AT     = CURRENT_TIMESTAMP,
            UPDATED_BY     = #{updatedByLoginId},
            VERSION_NO     = VERSION_NO + 1
        WHERE AUTH_USER_ID = #{id, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.AuthUserIdTypeHandler}
          AND VERSION_NO   = #{versionNo}
    </update>

</mapper>
```

> ※ここは「設計」なので、
>
> * TypeHandler のクラス名
> * AuthUser のフィールド構成（`createdByLoginId` etc）
    >   は実装フェーズで微調整する前提で、イメージとして記載しています。

---

## 2-4. Infrastructure 側 Repository 実装

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthUser;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;
import com.myou.ec.ecsite.domain.auth.model.value.LoginId;
import com.myou.ec.ecsite.domain.auth.repository.AuthUserRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthUserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisAuthUserRepository implements AuthUserRepository {

    private final AuthUserMapper mapper;

    public MybatisAuthUserRepository(AuthUserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<AuthUser> findById(AuthUserId authUserId) {
        AuthUser user = mapper.findById(authUserId.value());
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<AuthUser> findByLoginId(LoginId loginId) {
        AuthUser user = mapper.findByLoginId(loginId.value());
        return Optional.ofNullable(user);
    }

    @Override
    public AuthUserId nextId() {
        // IDENTITY 利用の場合は「登録後に getId() で取れる」ので、
        // nextId() を使わず、呼び出し側では null ID で insert してもよい。
        // ここでは「未使用」としてダミー実装にしておき、必要ならシーケンスを呼び出す Mapper を用意。
        throw new UnsupportedOperationException("IDENTITY strategy: use generated key on insert");
    }

    @Override
    public void save(AuthUser authUser) {
        if (authUser.id() == null) {
            mapper.insert(authUser);
        } else {
            mapper.update(authUser);
        }
    }
}
```

* **Repository 実装は Mapper を薄くラップするだけ**。
* Domain から見ると MyBatis の存在を意識しなくてよい構造になります。

---

# 3. 他テーブルの Repository & Mapper インタフェース案

ここからはパターンだけサクッと出します。
XML も基本的には AuthUser と同じノリで設計できます。

---

## 3-1. AuthRoleRepository / AuthRoleMapper

### Domain 側

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthRole;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.util.List;

public interface AuthRoleRepository {

    List<AuthRole> findAll();

    List<AuthRole> findByUserId(AuthUserId authUserId);

    List<AuthRole> findByCodes(List<RoleCode> roleCodes);
}
```

### Mapper インタフェース（infra）

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.domain.auth.model.AuthRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthRoleMapper {

    List<AuthRole> findAll();

    List<AuthRole> findByUserId(@Param("authUserId") long authUserId);

    List<AuthRole> findByCodes(@Param("roleCodes") List<String> roleCodes);
}
```

---

## 3-2. AuthLoginHistoryRepository / AuthLoginHistoryMapper

### Domain 側

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;

import java.util.List;

public interface AuthLoginHistoryRepository {

    void save(LoginHistory history);

    List<LoginHistory> findRecentByUserId(AuthUserId authUserId, int limit);
}
```

### Mapper インタフェース（infra）

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthLoginHistoryMapper {

    void insert(LoginHistory history);

    List<LoginHistory> findRecentByUserId(@Param("authUserId") long authUserId,
                                          @Param("limit") int limit);
}
```

---

## 3-3. AuthAccountLockHistoryRepository / AuthAccountLockHistoryMapper

### Domain 側

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;

import java.util.Optional;

public interface AuthAccountLockHistoryRepository {

    void save(AccountLockEvent event);

    Optional<AccountLockEvent> findLatestByUserId(AuthUserId authUserId);
}
```

### Mapper インタフェース（infra）

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthAccountLockHistoryMapper {

    void insert(AccountLockEvent event);

    AccountLockEvent findLatestByUserId(@Param("authUserId") long authUserId);
}
```

---

## 3-4. AuthPasswordHistoryRepository / AuthPasswordHistoryMapper

### Domain 側

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthUserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthPasswordHistoryRepository {

    void save(PasswordHistory history);

    List<PasswordHistory> findRecentByUserId(AuthUserId authUserId, int limit);

    Optional<PasswordHistory> findLastByUserId(AuthUserId authUserId);
}
```

### Mapper インタフェース（infra）

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthPasswordHistoryMapper {

    void insert(PasswordHistory history);

    List<PasswordHistory> findRecentByUserId(@Param("authUserId") long authUserId,
                                             @Param("limit") int limit);

    PasswordHistory findLastByUserId(@Param("authUserId") long authUserId);
}
```

---

# 4. TypeHandler の設計方針（チラ見せ）

ValueObject を MyBatis で扱うために、
`infrastructure.mybatis.type` 配下に TypeHandler を定義しておくとスッキリします。

例：

```java
// BIGINT ↔ AuthUserId
public class AuthUserIdTypeHandler extends BaseTypeHandler<AuthUserId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AuthUserId parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setLong(i, parameter.value());
    }

    @Override
    public AuthUserId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : new AuthUserId(value);
    }

    // 他オーバーライドも同様
}
```

同様に：

* `LoginIdTypeHandler`（VARCHAR ↔ LoginId）
* `EncodedPasswordTypeHandler`（VARCHAR ↔ EncodedPassword）
* `RoleCodeTypeHandler`（VARCHAR ↔ RoleCode）
* `BooleanFlagTypeHandler`（CHAR('1'/'0') ↔ boolean）

などを用意して、Mapper XML で `typeHandler` に指定するイメージです。

---

# 5. まとめ

* **Domain**：

    * `com.myou.ec.ecsite.domain.auth.repository.*` に Repository インタフェース
    * VO / Entity ベースでメソッドを定義

* **Infrastructure**：

    * `com.myou.ec.ecsite.infrastructure.auth.mapper.*` に MyBatis Mapper インタフェース
    * `com.myou.ec.ecsite.infrastructure.auth.repository.*` に Repository 実装
    * Mapper XML で DDL と Domain モデルを結びつける
    * ValueObject 用の TypeHandler で型変換を吸収

* 詳細な XML はまず AUTH_USER だけしっかり作り、
  他テーブルは同じパターンで増やしていく、という進め方が現実的です。
