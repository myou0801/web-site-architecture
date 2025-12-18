# MyBatis（クエリ設計/Mapper XML雛形/インデックス）

本書は、再構築後のDB（`AUTH_ACCOUNT`＝現在値、`*_HISTORY`＝insert-only）に対応した MyBatis 設計です。

---

## 1. 必要クエリ（Q1〜Q10）

- (Q1) account by user_id（status判断用に account_status まで取得）
- (Q2) account by account_id
- (Q3) insert AUTH_ACCOUNT（ID採番は INSERT 後に user_id で再取得）
- (Q4) update password_hash（楽観ロック：version一致）
- (Q5) update account_status（楽観ロック：version一致）
- (Q6) roles：accountId → role_codes（`AUTH_ROLE.enabled=true` のみ）
- (Q7) role_id by role_code（付与/剥奪用）
- (Q8) latest SUCCESS login_at
- (Q9) consecutive failure 用：直近 N 件（SUCCESS/FAILURE）
- (Q10) latest lock / expiry / password history

> 重要：ロック/期限切れは `AUTH_ACCOUNT` の列で保持しない方針のため、最新イベント取得（LOCK/UNLOCK、EXPIRE/UNEXPIRE）が必要です。
> 往復を減らしたい場合は `AUTH_ACCOUNT_CURRENT` VIEW（PostgreSQL）を併用してください。

---

## 2. インデックス（要点）

- `AUTH_ACCOUNT(user_id)`：UNIQUE
- `AUTH_ACCOUNT_STATUS_HISTORY(auth_account_id, occurred_at desc, id desc)`：最近履歴
- `AUTH_LOGIN_HISTORY(auth_account_id, login_at desc, id desc)`：最新参照・直近N件
- `AUTH_ACCOUNT_LOCK_HISTORY(auth_account_id, occurred_at desc, id desc)`：最新ロック
- `AUTH_ACCOUNT_EXPIRY_HISTORY(auth_account_id, occurred_at desc, id desc)`：最新期限
- `AUTH_PASSWORD_HISTORY(auth_account_id, changed_at desc, id desc)`：最新/直近N件
- `AUTH_ROLE(role_code)`：UNIQUE
- `AUTH_ACCOUNT_ROLE(auth_account_id)`：付与ロール参照

---

## 3. Mapper XML 雛形

以下は「Record（DB行）を返す」レベルの雛形です（Domain 変換は repository 側で実施）。

### 3.1 AuthAccountMapper.xml

```xml
<mapper namespace="com.example.infra.mybatis.AuthAccountMapper">

  <resultMap id="AuthAccountRecordMap" type="com.example.infra.mybatis.record.AuthAccountRecord">
    <id     property="authAccountId" column="auth_account_id"/>
    <result property="userId"        column="user_id"/>
    <result property="passwordHash"  column="password_hash"/>
    <result property="accountStatus" column="account_status"/>
    <result property="createdAt"     column="created_at"/>
    <result property="createdBy"     column="created_by"/>
    <result property="updatedAt"     column="updated_at"/>
    <result property="updatedBy"     column="updated_by"/>
    <result property="version"       column="version"/>
  </resultMap>

  <!-- (Q1) user_id で取得（DISABLED/DELETED も含めて返し、アプリ側で拒否理由を判断） -->
  <select id="selectByUserId" resultMap="AuthAccountRecordMap">
    SELECT auth_account_id, user_id, password_hash, account_status,
           created_at, created_by, updated_at, updated_by, version
    FROM auth_account
    WHERE user_id = #{userId}
  </select>

  <!-- INSERT後のID取得用途（Record immutability対策） -->
  <select id="selectAccountIdByUserId" resultType="long">
    SELECT auth_account_id
    FROM auth_account
    WHERE user_id = #{userId}
  </select>

  <select id="selectByAccountId" resultMap="AuthAccountRecordMap">
    SELECT auth_account_id, user_id, password_hash, account_status,
           created_at, created_by, updated_at, updated_by, version
    FROM auth_account
    WHERE auth_account_id = #{authAccountId}
  </select>

  <!-- (Q3) IDは INSERT後に user_id で再取得する -->
  <insert id="insertAccount">
    INSERT INTO auth_account (
      user_id, password_hash, account_status,
      created_at, created_by, updated_at, updated_by, version
    ) VALUES (
      #{userId}, #{passwordHash}, #{accountStatus},
      CURRENT_TIMESTAMP, #{createdBy}, CURRENT_TIMESTAMP, #{updatedBy}, 1
    )
  </insert>

  <!-- (Q4) パスワード変更：楽観ロック（version一致） -->
  <update id="updatePasswordHash">
    UPDATE auth_account
    SET password_hash = #{passwordHash},
        updated_at = CURRENT_TIMESTAMP,
        updated_by = #{updatedBy},
        version = version + 1
    WHERE auth_account_id = #{authAccountId}
      AND version = #{version}
  </update>

  <!-- (Q5) 状態変更（ACTIVE/DISABLED/DELETED）：楽観ロック（version一致） -->
  <update id="updateAccountStatus">
    UPDATE auth_account
    SET account_status = #{accountStatus},
        updated_at = CURRENT_TIMESTAMP,
        updated_by = #{updatedBy},
        version = version + 1
    WHERE auth_account_id = #{authAccountId}
      AND version = #{version}
  </update>

</mapper>
```

> `updatePasswordHash` / `updateAccountStatus` が **0件更新**の場合は、アプリ側で `OptimisticLockException` 相当として扱うことを推奨します。

---

### 3.2 AuthAccountStatusHistoryMapper.xml

```xml
<mapper namespace="com.example.infra.mybatis.AuthAccountStatusHistoryMapper">

  <insert id="insertHistory">
    INSERT INTO auth_account_status_history (
      auth_account_id, from_status, to_status, reason,
      occurred_at, operated_by, created_at
    ) VALUES (
      #{authAccountId}, #{fromStatus}, #{toStatus}, #{reason},
      #{occurredAt}, #{operatedBy}, CURRENT_TIMESTAMP
    )
  </insert>

  <select id="selectRecentByAccountId" resultType="com.example.infra.mybatis.record.AuthAccountStatusHistoryRecord">
    SELECT auth_account_status_history_id, auth_account_id, from_status, to_status, reason,
           occurred_at, operated_by, created_at
    FROM auth_account_status_history
    WHERE auth_account_id = #{authAccountId}
    ORDER BY occurred_at DESC, auth_account_status_history_id DESC
    LIMIT #{limit}
  </select>

</mapper>
```

---

### 3.3 AuthLoginHistoryMapper.xml

```xml
<mapper namespace="com.example.infra.mybatis.AuthLoginHistoryMapper">

  <insert id="insertHistory">
    INSERT INTO auth_login_history (
      auth_account_id, result, login_at, remote_ip, user_agent, created_at
    ) VALUES (
      #{authAccountId}, #{result}, #{loginAt}, #{remoteIp}, #{userAgent}, CURRENT_TIMESTAMP
    )
  </insert>

  <!-- (Q8) latest SUCCESS -->
  <select id="selectLatestSuccessLoginAt" resultType="java.time.Instant">
    SELECT login_at
    FROM auth_login_history
    WHERE auth_account_id = #{authAccountId}
      AND result = 'SUCCESS'
    ORDER BY login_at DESC, auth_login_history_id DESC
    LIMIT 1
  </select>

  <!-- (Q9) recent attempts for consecutive failure policy -->
  <select id="selectRecentSuccessFailure" resultType="com.example.infra.mybatis.record.AuthLoginHistoryRecord">
    SELECT auth_login_history_id, auth_account_id, result, login_at, remote_ip, user_agent, created_at
    FROM auth_login_history
    WHERE auth_account_id = #{authAccountId}
      AND result IN ('SUCCESS','FAILURE')
    ORDER BY login_at DESC, auth_login_history_id DESC
    LIMIT #{limit}
  </select>

</mapper>
```

---

### 3.4 AuthAccountLockHistoryMapper.xml / AuthAccountExpiryHistoryMapper.xml

```xml
<mapper namespace="com.example.infra.mybatis.AuthAccountLockHistoryMapper">
  <insert id="insertHistory">
    INSERT INTO auth_account_lock_history (
      auth_account_id, event_type, reason, occurred_at, operated_by, created_at
    ) VALUES (
      #{authAccountId}, #{eventType}, #{reason}, #{occurredAt}, #{operatedBy}, CURRENT_TIMESTAMP
    )
  </insert>

  <select id="selectLatestEvent" resultType="com.example.infra.mybatis.record.AuthAccountLockHistoryRecord">
    SELECT auth_account_lock_history_id, auth_account_id, event_type, reason, occurred_at, operated_by, created_at
    FROM auth_account_lock_history
    WHERE auth_account_id = #{authAccountId}
    ORDER BY occurred_at DESC, auth_account_lock_history_id DESC
    LIMIT 1
  </select>
</mapper>

<mapper namespace="com.example.infra.mybatis.AuthAccountExpiryHistoryMapper">
  <insert id="insertHistory">
    INSERT INTO auth_account_expiry_history (
      auth_account_id, event_type, reason, occurred_at, operated_by, created_at
    ) VALUES (
      #{authAccountId}, #{eventType}, #{reason}, #{occurredAt}, #{operatedBy}, CURRENT_TIMESTAMP
    )
  </insert>

  <select id="selectLatestEvent" resultType="com.example.infra.mybatis.record.AuthAccountExpiryHistoryRecord">
    SELECT auth_account_expiry_history_id, auth_account_id, event_type, reason, occurred_at, operated_by, created_at
    FROM auth_account_expiry_history
    WHERE auth_account_id = #{authAccountId}
    ORDER BY occurred_at DESC, auth_account_expiry_history_id DESC
    LIMIT 1
  </select>
</mapper>
```

---

### 3.5 AuthPasswordHistoryMapper.xml

```xml
<mapper namespace="com.example.infra.mybatis.AuthPasswordHistoryMapper">

  <insert id="insertHistory">
    INSERT INTO auth_password_history (
      auth_account_id, change_type, changed_at, password_hash, operated_by, created_at
    ) VALUES (
      #{authAccountId}, #{changeType}, #{changedAt}, #{passwordHash}, #{operatedBy}, CURRENT_TIMESTAMP
    )
  </insert>

  <select id="selectLatestByAccountId" resultType="com.example.infra.mybatis.record.AuthPasswordHistoryRecord">
    SELECT auth_password_history_id, auth_account_id, change_type, changed_at, password_hash, operated_by, created_at
    FROM auth_password_history
    WHERE auth_account_id = #{authAccountId}
    ORDER BY changed_at DESC, auth_password_history_id DESC
    LIMIT 1
  </select>

  <select id="selectRecentByAccountId" resultType="com.example.infra.mybatis.record.AuthPasswordHistoryRecord">
    SELECT auth_password_history_id, auth_account_id, change_type, changed_at, password_hash, operated_by, created_at
    FROM auth_password_history
    WHERE auth_account_id = #{authAccountId}
    ORDER BY changed_at DESC, auth_password_history_id DESC
    LIMIT #{limit}
  </select>

</mapper>
```

---

### 3.6 AuthRoleMapper.xml / AuthAccountRoleMapper.xml

```xml
<mapper namespace="com.example.infra.mybatis.AuthRoleMapper">

  <select id="selectRoleIdByCode" resultType="long">
    SELECT auth_role_id
    FROM auth_role
    WHERE role_code = #{roleCode}
  </select>

  <select id="selectRoleCodesByAccountId" resultType="string">
    SELECT r.role_code
    FROM auth_account_role ar
    JOIN auth_role r ON r.auth_role_id = ar.auth_role_id
    WHERE ar.auth_account_id = #{authAccountId}
      AND r.enabled = TRUE
    ORDER BY r.role_code
  </select>

</mapper>

<mapper namespace="com.example.infra.mybatis.AuthAccountRoleMapper">

  <insert id="insertAccountRole">
    INSERT INTO auth_account_role (auth_account_id, auth_role_id, created_at, created_by)
    VALUES (#{authAccountId}, #{authRoleId}, CURRENT_TIMESTAMP, #{createdBy})
  </insert>

  <delete id="deleteAccountRole">
    DELETE FROM auth_account_role
    WHERE auth_account_id = #{authAccountId}
      AND auth_role_id = #{authRoleId}
  </delete>

</mapper>
```

---

## 4. 認証判定の参照パターン（アプリ側の指針）

認証時に必要な判定は以下の順で実施することを推奨します。

1. (Q1) `AUTH_ACCOUNT` を user_id で取得（存在しない場合は同一メッセージで失敗）
2. `account_status` が `ACTIVE` 以外なら拒否（DISABLED/DELETED）
3. (必要なら) 最新ロックイベントが `LOCK` なら拒否（解除は管理者のみ仕様）
4. (必要なら) 最新期限イベントが `EXPIRE` なら拒否
5. パスワード照合 → 成否に応じて `AUTH_LOGIN_HISTORY` を insert-only

