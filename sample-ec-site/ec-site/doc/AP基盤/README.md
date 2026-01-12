# AP基盤（認証）設計まとめ

このドキュメントは、AP基盤（認証コンテキスト）における仕様、設計方針、および実装ルールをまとめたものです。
クラス構成の詳細は `AP基盤のクラス構成.md` を参照してください。

---

## 3. 認証・認可の仕様（確定事項）

### 3.1 ログイン

- 入力は **ログインID + パスワード**
- 認証成功でメニューへ（ただし後述の「パスワード変更必須」の場合は変更画面へ誘導）

### 3.2 ロール

- **1アカウント複数ロール** を許容
- DBは中間テーブル（N:M）で管理

設計方針:

- ドメインの `AuthAccount` にロールを内包しない（認証の核に責務を閉じる）
- ロールは `AuthAccountRoleRepository`（または QueryMapper）で取得し、`presentation`層の `AuthAccountDetails` の
  `GrantedAuthority` に反映する

### 3.3 パスワードポリシー（最新版）

- 最小桁数: **12**
- 文字種: **英大文字 / 英小文字 / 数字 / 記号** のうち **3種類以上**
- 記号の許可集合: `#$%()+=?@*[]{}|\`
- **UserId とパスワードの完全一致は禁止**
- 有効期限: **90日**
- 履歴世代: **3世代**（直近3回と同じパスワード禁止）
- ロールによりUserId規約が異なる可能性あり（詳細設計で決定）

補足:

- このポリシーは `domain.auth.policy` パッケージ配下の `PasswordPolicy` 実装クラス群によって実現されます。
- 画面にポリシー文言を出すかは業務T判断ですが、AP基盤はサーバ側で必ず検証します。

### 3.4 パスワード初期化

- 管理者ロールがアカウント管理で実施
- 初期パスワードは固定（例: `password123`。本番値は別途決定）
- 初期化後、次回ログイン成功時は **必ずパスワード変更画面へ**
- **パスワード初期化するとロックも解除**する

### 3.5 アカウントロック

- **6回失敗でロック**
- ロック解除は **管理者が明示的に解除**（期限解除はしない。将来拡張としては検討余地あり）
- **パスワード初期化するとロックも解除**
- ロック中のログインは履歴に残す
    - `AUTH_LOGIN_HISTORY.result = 'LOCKED'`
    - ただし **失敗カウントには含めない**

### 3.6 前回ログイン日時

- ログイン後に前回ログイン日時を画面共通ヘッダに表示
- `presentation`層の `AuthAccountDetails` に「前回ログイン日時（LocalDateTime）」を保持させ、業務Tが参照できるようにする

---

## 4. 設計方針（重要）

### 4.1 UPDATEを減らす（履歴主導）

- 可能な限り **履歴（insert-only）** を積む
- ただし、`AUTH_ACCOUNT` / `AUTH_ROLE` は更新が必要（例: `password_hash` 等）
- アカウントの有効/無効/削除は、`AUTH_ACCOUNT.account_status`（ACTIVE/DISABLED/DELETED）で表現する
    - 旧 `enabled` / `deleted` のようなフラグ分割は採用しない

### 4.2 同時実行（レース）・ロックイベント重複

- 方針A（割り切り）:
    - ロック/解除イベントの重複は許容
    - 現在状態は「最新イベント」で判断（`ORDER BY occurred_at DESC, id DESC LIMIT 1` 相当）

### 4.3 トランザクション境界

- 原則、業務Tの Service に `@Transactional`
- ただし、業務Tから呼ばれない入口などは SharedService 側に `@Transactional` を付けてもよい（入口境界を明確にする）

### 4.4 created_by / operated_by の扱い

- `created_at`：DB側で `DEFAULT CURRENT_TIMESTAMP`（TIMESTAMP）を付与
- `created_by`：**ドメインオブジェクトには持たない**
    - insert 系Repositoryの引数として `Operator`（操作者）を受け取り、`created_by` に設定する
- `operated_by`：履歴系（ロック/期限/状態変更/パスワード変更 等）の操作者として保存する
- 呼び出し元（application層）は `CurrentUserProvider` / `SecurityContext` から操作者を取得して引数に渡す

---

## 5. DB設計（AP基盤スキーマ）

### 5.1 監査列ルール（確定）

- 原則: **全テーブルに `created_at` + `created_by`（NOT NULL）**
- 更新があるテーブルのみ: **`updated_at` + `updated_by`（NOT NULL）**
- 履歴系（insert-only）は `updated_*` を持たない

### 5.2 テーブル一覧（最新版）

- `AUTH_ACCOUNT`（更新あり）
    - `account_status`（ACTIVE/DISABLED/DELETED）
- `AUTH_ROLE`（更新あり）
- `AUTH_ACCOUNT_ROLE`（中間：insert/delete中心）
- `AUTH_PASSWORD_HISTORY`（insert-only）
- `AUTH_LOGIN_HISTORY`（insert-only）
- `AUTH_ACCOUNT_LOCK_HISTORY`（insert-only）
    - `locked BOOLEAN NOT NULL`（TRUE=LOCK / FALSE=UNLOCK）
    - `reason VARCHAR(64) NOT NULL`
- `AUTH_ACCOUNT_EXPIRY_HISTORY`（insert-only）
    - `event_type`（EXPIRE/UNEXPIRE）
    - `reason VARCHAR(64) NOT NULL`
- `AUTH_ACCOUNT_STATUS_HISTORY`（insert-only）
    - `from_status` / `to_status`
    - `reason VARCHAR(64) NOT NULL`

### 5.3 Query sharedService 用 VIEW（参照専用）

Query sharedService は、以下の VIEW を前提とする（一覧 + ロールIN一括取得）。

- `AUTH_ACCOUNT_CURRENT_V`
    - `locked` / `expired` / `last_login_at` を「最新履歴」から導出した参照VIEW
- `AUTH_ACCOUNT_ROLE_V`
    - `auth_account_id` と `role_code` の行形式VIEW

DB方言差:

- PostgreSQL（本番）：`DISTINCT ON` で最新1件抽出
- H2（開発）：`row_number()` で最新1件抽出（`DROP VIEW IF EXISTS` + `CREATE VIEW`）

---

## 6. 認証イベント処理方針（Spring Security）

`presentation`層の `auth.security.event` パッケージに配置されたリスナーが、Spring Security の認証イベントを処理します。

- ログイン成功:
    - `AuthenticationSuccessEvent` リスナーで `AUTH_LOGIN_HISTORY(SUCCESS)` をinsert
    - 前回ログイン日時の組み立てに使う
- ログイン失敗:
    - `AbstractAuthenticationFailureEvent` 由来のイベントで `AUTH_LOGIN_HISTORY(FAILURE)` をinsert
    - 失敗回数・連続判定は履歴から算出
- ロック中:
    - `AUTH_LOGIN_HISTORY(LOCKED)` をinsert（失敗カウント対象外）
- 無効（DISABLED）:
    - `AUTH_LOGIN_HISTORY(DISABLED)` をinsert（必要なら）

入力UserIdの取得:

- failureイベントから username が取れない場合があるため、`presentation.auth.security.UserIdFactory` を介してリクエストから取得します。

---

## 7. パスワード変更必須判定の配置

- 履歴登録（成功/失敗）はイベントリスナーで行う
- **パスワード変更必須チェック**は以下を採用:
    - `presentation.auth.security.handler.AuthAuthenticationSuccessHandler` や
      `presentation.auth.security.interceptor.PasswordChangeRequiredInterceptor` で実施（セッションにフラグを置かない）
    - 無限ループ回避の除外URLは外部設定化
- `AuthAuthenticationSuccessHandler` は `SavedRequestAwareAuthenticationSuccessHandler` を extends し、
    - `defaultSuccessUrl` を SecurityConfig から設定する

---

## 8. MyBatis / Infrastructure 実装ルール

- TypeHandlerは使わない
- DB ↔ Domain 変換は **RepositoryImpl** が担う
- レコードは `*Record`（`DbRecord` という命名は使用しない）
- Repository実装クラス名: `XxxRepositoryImpl`（`MybatisXxx` は使わない）
- MyBatis:
    - Mapper interface + XML を採用（アノテーションSQLは使わない）
    - `resources/com/myou/ec/ecsite/infrastructure/auth/mapper/*.xml` に配置
    - keyGeneratorは利用しない

---

## 9. 業務T向け提供I/F（sharedService）

### 9.1 Command系（状態変更）

- **AuthAccountAdminSharedService**
    - 管理者操作（有効化/無効化/論理削除、ロック解除、パスワード初期化 等）を提供します。状態変更と履歴登録を同一トランザクションで実施します。

### 9.2 Query系（参照：VIEW前提）

- **AuthAccountQuerySharedService**（Query／VIEW前提）
    - アカウント一覧検索（ページング）
    - `user_id` 指定の単一取得
    - 返却DTOには機密情報（`password_hash` 等）を含めない。
    - 返却DTOには **ロール** を含める（一覧は **IN で一括取得**してDTOへマージ）。
    - 日時型は **LocalDateTime（JSTローカル日時）** とする。