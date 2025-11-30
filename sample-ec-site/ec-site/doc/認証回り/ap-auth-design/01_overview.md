# AP基盤（認証）設計まとめ v1（分割版）

対象：
- Spring MVC（Spring Bootなし）
- Spring Framework 6.2
- Spring Security 6.5
- MyBatis
- DB：PostgreSQL（本番）、H2（開発）
- Java：25

本ドキュメントは、AP基盤Tが提供する「認証共通部品」の設計を、業務Tとの契約（I/F・例外・メッセージキー）まで含めて整理したものです。

---

## 0. 分担（AP基盤T / 業務T）

### AP基盤T
- Spring Security 統合（SecurityConfig、UserDetailsService、Handler/Interceptor、イベントリスナー）
- 認証系DB（AUTH_*）の設計・永続化（MyBatis）
- sharedService 提供（管理者操作、パスワード変更、ユーザコンテキスト）
- DDDを意識したDomain（Value/Entity/Policy/例外）の提供

### 業務T
- 画面（login/menu/password-change/admin）
- Controller / 業務Service（sharedService呼び出し）
- messages.properties（キーに対する文言）
- 画面固有のバリデーション（confirm不一致など）

---

## 1. 用語・命名（混同防止）

### 画面項目（仕様）
- 画面入力：**ユーザID**（内部IDと混同しない名称）
- リクエストパラメータ名：`userId`
- パスワードパラメータ名：`password`

### 内部不変ID（認証基盤）
- 内部ID：**アカウントID**
- DBカラム：`auth_account_id`
- Java VO：`AuthAccountId`

### ログインに使うID
- DBカラム：`user_id`（UNIQUE）
- Java VO：`UserId`

### 監査列（*_by）
- `created_by / updated_by / deleted_by` は **操作者UserId（文字列）**を格納
- `AuthAccountId` は使わない（必要になったら別列で追加）

---

## 2. 仕様（確定）

### 2.1 パスワードポリシー
- 最小桁数：5
- 文字種：英数字（`[0-9A-Za-z]+`）
- ログインID（ユーザID）とパスワード完全一致は禁止
- 有効期限：90日
- 世代：3世代（過去3件と同一禁止）

### 2.2 初期パスワード（管理者初期化含む）
- 固定：`password123`（後で調整可能）
- 初期化後はログイン成功時、必ずパスワード変更へ誘導

### 2.3 ロックアウト
- 連続失敗 6回でロック
- 解除は管理者操作のみ
- 初期化（resetPassword）でロック解除も実施
- ロック中ログインは `AUTH_LOGIN_HISTORY.result='LOCKED'` で履歴を残す（失敗カウントはしない）

### 2.4 前回ログイン日時
- 「前回ログイン日時」＝今回成功の直前に成功していた日時（直前のSUCCESS）
- `AuthUserDetails.previousLoginAt` に保持し業務Tが共通ヘッダ等に表示

### 2.5 パスワード変更強制
- 初回登録 / 管理者初期化後 / 有効期限切れ（90日超過）の場合：
  - ログイン成功後に **パスワード変更画面へ**
- セッションフラグは使わず、都度 `requirementOf` で判定

### 2.6 ロール
- 1アカウント複数ロール（将来拡張を前提）
- authority は `ROLE_` + `role_code`（例：`ADMIN` → `ROLE_ADMIN`）

---

## 3. 設定値（application.properties/yml管理）

推奨キー例：
- `auth.password.min-length=5`
- `auth.password.allowed-pattern=^[0-9A-Za-z]+$`
- `auth.password.expire-days=90`
- `auth.password.history-generations=3`
- `auth.lock.failure-threshold=6`
- `auth.initial-password=password123`
- `auth.default-success-url=/menu`
- `auth.pwchange.bypass-patterns=/login,/logout,/password/change/**,/css/**,/js/**,/images/**,/webjars/**,/error`
