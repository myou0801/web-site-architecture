じゃあここからは、**MyBatis Mapper と Repository インタフェースの「設計」**をまとめていきますね。
細かい SQL も含めて、まずは **AUTH_ACCOUNT 周りをフルで例示**して、残りはパターンだけ押さえる形にします。

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

# 2. AUTH_ACCOUNT：詳細なパターン

## 2-1. Domain 側 Repository インタフェース

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.util.Optional;

public interface AuthAccountRepository {

    Optional<AuthAccount> findById(AuthAccountId authAccountId);

    Optional<AuthAccount> findByUserId(UserId userId);

    AuthAccountId nextId();                 // 採番（IDENTITY を使うなら不要でもOK）

    void save(AuthAccount authAccount);        // insert or update を内部で判定
}
```

* Domain 層からは **VO / Entity だけ見える**。
* `save` 内で「新規か更新か」は `authAccount.id()` の有無等で判定。

---

## 2-2. Infrastructure 側 Mapper インタフェース（MyBatis）

```java
package com.myou.ec.ecsite.infrastructure.auth.mapper;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthAccountMapper {

    AuthAccount findById(@Param("authAccountId") long authAccountId);

    AuthAccount findByUserId(@Param("userId") String userId);

    void insert(AuthAccount authAccount);

    void update(AuthAccount authAccount);
}
```

* MyBatis の Mapper は **インフラ側で domain の AuthAccount を直接扱う**想定。
* VO（`AuthAccountId`, `UserId`, `EncodedPassword` など）は
  TypeHandler で DB と変換するか、`AuthAccountMapper` 側で `getValue()` を呼ぶかのどちらか。

ここでは **XML で constructor + TypeHandler を使う方式**を想定します。

---

## 2-3. AuthAccountMapper.xml の例

`src/main/resources/mybatis/auth/AuthAccountMapper.xml` など。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper">

    <!-- ValueObject / boolean 用の TypeHandler を使う前提例
         - AuthAccountIdTypeHandler      : BIGINT ↔ AuthAccountId
         - UserIdTypeHandler         : VARCHAR ↔ UserId
         - EncodedPasswordTypeHandler : VARCHAR ↔ EncodedPassword
         - BooleanFlagTypeHandler     : CHAR('1'/'0') ↔ boolean
    -->

    <resultMap id="AuthAccountResultMap" type="com.myou.ec.ecsite.domain.auth.model.AuthAccount">

        <constructor>
            <arg column="AUTH_ACCOUNT_ID"
                 javaType="com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId"
                 typeHandler="com.myou.ec.ecsite.infrastructure.mybatis.type.AuthAccountIdTypeHandler"/>

            <arg column="USER_ID"
                 javaType="com.myou.ec.ecsite.domain.auth.model.value.UserId"
                 typeHandler="com.myou.ec.ecsite.infrastructure.mybatis.type.UserIdTypeHandler"/>

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

    <select id="findById" resultMap="AuthAccountResultMap">
        SELECT
            AUTH_ACCOUNT_ID,
            USER_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        FROM AUTH_ACCOUNT
        WHERE AUTH_ACCOUNT_ID = #{authAccountId}
          AND DELETED_FLG = '0'
    </select>

    <select id="findByUserId" resultMap="AuthAccountResultMap">
        SELECT
            AUTH_ACCOUNT_ID,
            USER_ID,
            LOGIN_PASSWORD,
            ENABLED_FLG,
            CREATED_AT,
            CREATED_BY,
            UPDATED_AT,
            UPDATED_BY,
            VERSION_NO
        FROM AUTH_ACCOUNT
        WHERE USER_ID = #{userId}
          AND DELETED_FLG = '0'
    </select>

    <insert id="insert" parameterType="com.myou.ec.ecsite.domain.auth.model.AuthAccount"
            useGeneratedKeys="true" keyProperty="id.value">
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
            #{userId,  typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.UserIdTypeHandler},
            #{encodedPassword, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.EncodedPasswordTypeHandler},
            #{enabled,  typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.BooleanFlagTypeHandler},
            '0',
            CURRENT_TIMESTAMP,
            #{createdByUserId},
            CURRENT_TIMESTAMP,
            #{createdByUserId},
            0
        )
    </insert>

    <update id="update" parameterType="com.myou.ec.ecsite.domain.auth.model.AuthAccount">
        UPDATE AUTH_ACCOUNT
        SET
            USER_ID       = #{userId, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.UserIdTypeHandler},
            LOGIN_PASSWORD = #{encodedPassword, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.EncodedPasswordTypeHandler},
            ENABLED_FLG    = #{enabled, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.BooleanFlagTypeHandler},
            UPDATED_AT     = CURRENT_TIMESTAMP,
            UPDATED_BY     = #{updatedByUserId},
            VERSION_NO     = VERSION_NO + 1
        WHERE AUTH_ACCOUNT_ID = #{id, typeHandler=com.myou.ec.ecsite.infrastructure.mybatis.type.AuthAccountIdTypeHandler}
          AND VERSION_NO   = #{versionNo}
    </update>

</mapper>
```

> ※ここは「設計」なので、
>
> * TypeHandler のクラス名
> * AuthAccount のフィールド構成（`createdByUserId` etc）
    >   は実装フェーズで微調整する前提で、イメージとして記載しています。

---

## 2-4. Infrastructure 側 Repository 実装

```java
package com.myou.ec.ecsite.infrastructure.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AuthAccount;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.repository.AuthAccountRepository;
import com.myou.ec.ecsite.infrastructure.auth.mapper.AuthAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisAuthAccountRepository implements AuthAccountRepository {

    private final AuthAccountMapper mapper;

    public MybatisAuthAccountRepository(AuthAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<AuthAccount> findById(AuthAccountId authAccountId) {
        AuthAccount user = mapper.findById(authAccountId.value());
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<AuthAccount> findByUserId(UserId userId) {
        AuthAccount user = mapper.findByUserId(userId.value());
        return Optional.ofNullable(user);
    }

    @Override
    public AuthAccountId nextId() {
        // IDENTITY 利用の場合は「登録後に getId() で取れる」ので、
        // nextId() を使わず、呼び出し側では null ID で insert してもよい。
        // ここでは「未使用」としてダミー実装にしておき、必要ならシーケンスを呼び出す Mapper を用意。
        throw new UnsupportedOperationException("IDENTITY strategy: use generated key on insert");
    }

    @Override
    public void save(AuthAccount authAccount) {
        if (authAccount.id() == null) {
            mapper.insert(authAccount);
        } else {
            mapper.update(authAccount);
        }
    }
}
```

* **Repository 実装は Mapper を薄くラップするだけ**。
* Domain から見ると MyBatis の存在を意識しなくてよい構造になります。

---

# 3. 他テーブルの Repository & Mapper インタフェース案

ここからはパターンだけサクッと出します。
XML も基本的には AuthAccount と同じノリで設計できます。

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

    List<AuthRole> findByAccountId(AuthAccountId authAccountId);

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

    List<AuthRole> findByAccountId(@Param("authAccountId") long authAccountId);

    List<AuthRole> findByCodes(@Param("roleCodes") List<String> roleCodes);
}
```

---

## 3-2. AuthLoginHistoryRepository / AuthLoginHistoryMapper

### Domain 側

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.LoginHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

import java.util.List;

public interface AuthLoginHistoryRepository {

    void save(LoginHistory history);

    List<LoginHistory> findRecentByAccountId(AuthAccountId authAccountId, int limit);
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

    List<LoginHistory> findRecentByAccountId(@Param("authAccountId") long authAccountId,
                                          @Param("limit") int limit);
}
```

---

## 3-3. AuthAccountLockHistoryRepository / AuthAccountLockHistoryMapper

### Domain 側

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.AccountLockEvent;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

import java.util.Optional;

public interface AuthAccountLockHistoryRepository {

    void save(AccountLockEvent event);

    Optional<AccountLockEvent> findLatestByAccountId(AuthAccountId authAccountId);
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

    AccountLockEvent findLatestByAccountId(@Param("authAccountId") long authAccountId);
}
```

---

## 3-4. AuthPasswordHistoryRepository / AuthPasswordHistoryMapper

### Domain 側

```java
package com.myou.ec.ecsite.domain.auth.repository;

import com.myou.ec.ecsite.domain.auth.model.PasswordHistory;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthPasswordHistoryRepository {

    void save(PasswordHistory history);

    List<PasswordHistory> findRecentByAccountId(AuthAccountId authAccountId, int limit);

    Optional<PasswordHistory> findLastByAccountId(AuthAccountId authAccountId);
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

    List<PasswordHistory> findRecentByAccountId(@Param("authAccountId") long authAccountId,
                                             @Param("limit") int limit);

    PasswordHistory findLastByAccountId(@Param("authAccountId") long authAccountId);
}
```

---

# 4. TypeHandler の設計方針（チラ見せ）

ValueObject を MyBatis で扱うために、
`infrastructure.mybatis.type` 配下に TypeHandler を定義しておくとスッキリします。

例：

```java
// BIGINT ↔ AuthAccountId
public class AuthAccountIdTypeHandler extends BaseTypeHandler<AuthAccountId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AuthAccountId parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setLong(i, parameter.value());
    }

    @Override
    public AuthAccountId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : new AuthAccountId(value);
    }

    // 他オーバーライドも同様
}
```

同様に：

* `UserIdTypeHandler`（VARCHAR ↔ UserId）
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

* 詳細な XML はまず AUTH_ACCOUNT だけしっかり作り、
  他テーブルは同じパターンで増やしていく、という進め方が現実的です。
