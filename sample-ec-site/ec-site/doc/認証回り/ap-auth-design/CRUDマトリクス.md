# CRUDマトリクス（再構築DB対応）

本書は、再構築後のDB（`AUTH_ACCOUNT`＝現在値最小、`*_HISTORY`＝insert-only）に合わせて、
各テーブルの主な操作（CRUD）とユースケース上の役割を整理したものです。

---

## AP基盤（認証スキーマ）

### 1) AUTH_ACCOUNT（更新あり：現在値）

**役割**
認証に必要な「最小の現在値」を保持する。
- userId による特定
- パスワード照合のための最新ハッシュ
- 管理状態（ACTIVE/DISABLED/DELETED）

**主なユースケース**
- ログイン時：`user_id` で検索して `password_hash` と照合し、`account_status` を判定
- 管理者操作：アカウント作成、無効化/有効化（=account_status更新）、論理削除（=DELETEDへ遷移）、パスワード初期化（ハッシュ更新）

**主要カラム**
- `auth_account_id`：内部不変ID（PK）。業務テーブルとの紐付けキー
- `user_id`：画面の「ユーザID」（UNIQUE）
- `password_hash`：最新のパスワードハッシュ（生PWは保持しない）
- `account_status`：`ACTIVE / DISABLED / DELETED`
- `created_*/updated_*`：監査
- `version`：楽観ロック（推奨）

**CRUD**
- C：アカウント作成
- R：ログイン、管理参照
- U：パスワード更新、status更新
- D：物理削除は原則しない（必要なら別途運用）

**注意点**
- `account_status=DELETED` は存在しない扱い（UsernameNotFound）とする運用を推奨（ユーザ列挙対策）

---

### 2) AUTH_ACCOUNT_STATUS_HISTORY（insert-only：状態遷移履歴）

**役割**
`AUTH_ACCOUNT.account_status` の遷移を「事実」として残す（有効/無効/削除など）。

**CRUD**
- C：状態変更時に INSERT
- R：運用調査
- U/D：なし（insert-only）

---

### 3) AUTH_ROLE（更新あり：ロールマスタ）

**役割**
ロールコードと表示名、利用可否（enabled）を管理する。

**主要カラム**
- `auth_role_id`：PK
- `role_code`：ロール識別子（UNIQUE、例：ADMIN/USER）
- `enabled`：ロールの有効/無効

**CRUD**
- C/U：ロール定義の追加・更新
- R：認可評価時の参照

---

### 4) AUTH_ACCOUNT_ROLE（insert/delete中心：付与情報）

**役割**
アカウントとロールの対応（N:M）。

**CRUD**
- C：付与（INSERT）
- R：ログイン時にロール一覧取得
- D：剥奪（DELETE）

---

### 5) AUTH_PASSWORD_HISTORY（insert-only）

**役割**
パスワード変更の事実を保持する。
- 世代禁止（直近N件）
- 調査（いつ変更したか）

**CRUD**
- C：パスワード変更時に INSERT
- R：世代禁止判定、運用調査
- U/D：なし

---

### 6) AUTH_LOGIN_HISTORY（insert-only）

**役割**
ログイン試行結果の履歴。
- 前回ログイン日時の表示（SUCCESS）
- 連続失敗カウント（FAILURE）
- 調査（LOCKED/DISABLED/EXPIRED等の内訳）

**result**（推奨）
- `SUCCESS / FAILURE / LOCKED / DISABLED / EXPIRED`

**CRUD**
- C：Success/Failureイベントで INSERT
- R：連続失敗判定、期限切れ判定（lastSuccessAt）、運用調査
- U/D：なし

---

### 7) AUTH_ACCOUNT_LOCK_HISTORY（insert-only）

**役割**
ロック/解除の事実。
- 現仕様：解除は管理者のみ
- 現在のロック状態は「最新イベント（LOCK/UNLOCK）」で導出

**CRUD**
- C：自動ロック（SYSTEM）、管理者解除（UNLOCK）で INSERT
- R：ログイン時の状態判定（最新イベント）、運用調査
- U/D：なし

---

### 8) AUTH_ACCOUNT_EXPIRY_HISTORY（insert-only）

**役割**
期限切れ/解除の事実。
- 90日未ログイン等のポリシーで EXPIRE を積む
- 管理者操作で UNEXPIRE を積む

**CRUD**
- C：期限切れ確定（EXPIRE）、解除（UNEXPIRE）で INSERT
- R：ログイン時の期限切れ判定（最新イベント + baseAt）、運用調査
- U/D：なし

---

## 備考：運用上の整理
- 「現在値」：AUTH_ACCOUNT（account_status/password_hash）
- 「事実（履歴）」：各種 *_HISTORY（insert-only）
- ロック/期限切れの現在状態は履歴から導出し、必要なら VIEW（AUTH_ACCOUNT_CURRENT）で集約する
