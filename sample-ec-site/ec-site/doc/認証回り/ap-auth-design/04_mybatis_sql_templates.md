# MyBatis（クエリ設計/Mapper XML雛形/インデックス）

---

## 1. 必要クエリ（Q1〜Q7）
- (Q1) active account：`user_id = ? AND deleted=false`
- (Q2) roles：accountId → role_codes（`AUTH_ROLE.enabled=true` のみ）
- (Q3) latest SUCCESS login_at
- (Q4) consecutive failure用：`result in ('SUCCESS','FAILURE')` を直近7件程度
- (Q5) latest lock event（LOCK/UNLOCKの最新）
- (Q6) latest password change
- (Q7) recent N password history（世代チェック）

---

## 2. インデックス（要点）
- AUTH_ACCOUNT：UNIQUE(user_id)
- AUTH_LOGIN_HISTORY：(auth_account_id, login_at DESC) / (auth_account_id, result, login_at DESC)
- AUTH_PASSWORD_HISTORY：(auth_account_id, changed_at DESC)
- AUTH_ACCOUNT_LOCK_HISTORY：(auth_account_id, occurred_at DESC)
- AUTH_ACCOUNT_ROLE：PK(auth_account_id, role_code)

---

## 3. Mapper XML雛形

## 3.1 AuthAccountMapper.xml
```xml
<mapper namespace="...AuthAccountMapper">
  <select id="selectActiveByUserId" resultMap="AuthAccountRecordMap">
    SELECT * FROM auth_account
    WHERE user_id = #{userId} AND deleted = FALSE
  </select>
  <select id="selectByAccountId" resultMap="AuthAccountRecordMap">
    SELECT * FROM auth_account
    WHERE auth_account_id = #{authAccountId}
  </select>
  <insert id="insert">
    INSERT INTO auth_account(
      user_id, password_hash,
      enabled, deleted,
      created_at, created_by,
      updated_at, updated_by
    ) VALUES (
      #{userId}, #{passwordHash},
      TRUE, FALSE,
      #{createdAt}, #{createdBy},
      #{updatedAt}, #{updatedBy}
    )
  </insert>
  <update id="updatePasswordHash">
    UPDATE auth_account
    SET password_hash=#{passwordHash}, updated_at=#{updatedAt}, updated_by=#{updatedBy}
    WHERE auth_account_id=#{authAccountId}
  </update>
  <update id="updateEnabled">
    UPDATE auth_account
    SET enabled=#{enabled}, updated_at=#{updatedAt}, updated_by=#{updatedBy}
    WHERE auth_account_id=#{authAccountId} AND deleted=FALSE
  </update>
  <update id="markDeleted">
    UPDATE auth_account
    SET deleted=TRUE, deleted_at=#{deletedAt}, deleted_by=#{deletedBy},
        enabled=FALSE, updated_at=#{updatedAt}, updated_by=#{updatedBy}
    WHERE auth_account_id=#{authAccountId} AND deleted=FALSE
  </update>
</mapper>
```

## 3.2 AuthAccountRoleMapper.xml
```xml
<mapper namespace="...AuthAccountRoleMapper">
  <select id="selectRoleCodesByAccountId" resultType="string">
    SELECT ar.role_code
    FROM auth_account_role ar
    JOIN auth_role r ON r.role_code = ar.role_code
    WHERE ar.auth_account_id = #{authAccountId} AND r.enabled = TRUE
  </select>
  <insert id="insert">
    INSERT INTO auth_account_role(auth_account_id, role_code, created_at, created_by)
    VALUES (#{authAccountId}, #{roleCode}, #{createdAt}, #{createdBy})
  </insert>
  <delete id="delete">
    DELETE FROM auth_account_role
    WHERE auth_account_id = #{authAccountId} AND role_code = #{roleCode}
  </delete>
</mapper>
```

## 3.3 AuthLoginHistoryMapper.xml
```xml
<mapper namespace="...AuthLoginHistoryMapper">
  <insert id="insert">
    INSERT INTO auth_login_history(auth_account_id, result, login_at, created_at)
    VALUES (#{authAccountId}, #{result}, #{loginAt}, #{createdAt})
  </insert>
  <select id="selectLatestSuccessAt" resultType="java.time.LocalDateTime">
    SELECT login_at FROM auth_login_history
    WHERE auth_account_id=#{authAccountId} AND result='SUCCESS'
    ORDER BY login_at DESC LIMIT 1
  </select>
  <select id="selectRecentForConsecutiveFailure" resultMap="LoginHistoryRecordMap">
    SELECT auth_login_history_id, auth_account_id, result, login_at, created_at
    FROM auth_login_history
    WHERE auth_account_id=#{authAccountId} AND result IN ('SUCCESS','FAILURE')
    ORDER BY login_at DESC LIMIT #{limit}
  </select>
</mapper>
```

## 3.4 AuthAccountLockHistoryMapper.xml
```xml
<mapper namespace="...AuthAccountLockHistoryMapper">
  <insert id="insert">
    INSERT INTO auth_account_lock_history(auth_account_id, event_type, occurred_at, created_at)
    VALUES (#{authAccountId}, #{eventType}, #{occurredAt}, #{createdAt})
  </insert>
  <select id="selectLatestEvent" resultMap="LockHistoryRecordMap">
    SELECT auth_account_lock_history_id, auth_account_id, event_type, occurred_at, created_at
    FROM auth_account_lock_history
    WHERE auth_account_id=#{authAccountId}
    ORDER BY occurred_at DESC LIMIT 1
  </select>
</mapper>
```

## 3.5 AuthPasswordHistoryMapper.xml
```xml
<mapper namespace="...AuthPasswordHistoryMapper">
  <insert id="insert">
    INSERT INTO auth_password_history(auth_account_id, change_type, changed_at, password_hash, created_at)
    VALUES (#{authAccountId}, #{changeType}, #{changedAt}, #{passwordHash}, #{createdAt})
  </insert>
  <select id="selectLatestByAccountId" resultMap="PasswordHistoryRecordMap">
    SELECT auth_password_history_id, auth_account_id, change_type, changed_at, password_hash, created_at
    FROM auth_password_history
    WHERE auth_account_id=#{authAccountId}
    ORDER BY changed_at DESC LIMIT 1
  </select>
  <select id="selectRecentByAccountId" resultMap="PasswordHistoryRecordMap">
    SELECT auth_password_history_id, auth_account_id, change_type, changed_at, password_hash, created_at
    FROM auth_password_history
    WHERE auth_account_id=#{authAccountId}
    ORDER BY changed_at DESC LIMIT #{limit}
  </select>
</mapper>
```
