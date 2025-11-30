# 認証イベントリスナー（Success/Failure：履歴登録・ロック判定）

---

## 1. Successリスナー（AuthenticationSuccessEvent）
- `userId = auth.getName()`
- active accountが取れたら `LOGIN_HISTORY(SUCCESS)` をINSERT
- user not found/論理削除は何もしない（履歴なし）

---

## 2. Failureリスナー（AbstractAuthenticationFailureEvent）
- userIdは `event.getAuthentication().getName()`（RequestContextHolderは使わない）
- active accountが取れたら result を判定して `LOGIN_HISTORY` をINSERT
- FAILURE の場合のみ直近 `SUCCESS/FAILURE` を7件程度取得して連続失敗判定
- 閾値超えなら `LOCK_HISTORY(LOCK)` をINSERT
- LOCKED/DISABLED は履歴に残すが失敗カウント対象外

---

## 3. トランザクション/例外ログ（確定）
- Failure：`@Transactional`（LOGIN_HISTORY+LOCK_HISTORYを同一Tx）
- Success：統一のため `@Transactional` 推奨
- リスナー例外は認証成否に影響させないため握る（warnログ）
