了解です。いまのER/DDL前提（AP基盤＝認証、業務＝ユーザ詳細は仮）で、各テーブルの役割・代表ユースケース・主要カラムの意味・運用/注意点をまとめます。

---

## AP基盤（認証スキーマ）

### 1) AUTH_ACCOUNT（更新あり）

**役割**
認証の中心となる「アカウント本体」。ログイン可否・削除状態・パスワードハッシュを保持します。
履歴（ログイン/ロック/パスワード変更）は別テーブルに積み、AUTH_ACCOUNTは“現在の状態”の最小限だけを持つ位置づけです。

**主なユースケース**

* ログイン時：`user_id` で検索して `password_hash` と照合、`enabled/deleted` を判定
* 管理者操作：アカウント作成、無効化/有効化、削除（論理）、パスワード初期化（ハッシュ更新）

**主要カラム**

* `auth_account_id`：内部不変ID（PK）。業務テーブルとの紐付けキーにも使う
* `user_id`：画面の「ユーザID」（ユニーク）
* `password_hash`：パスワードハッシュ（生PWは持たない）
* `enabled`：利用可否（無効ならログイン不可→履歴にDISABLEDを記録）
* `deleted`：論理削除フラグ（削除されたらログイン不可）
* `created_at/by`：作成監査
* `updated_at/by`：更新監査（有効/無効、パスワード変更、削除フラグ更新で更新）
* `deleted_at/by`：論理削除操作の監査（deleted=trueのとき埋める）

**注意点**

* `user_id` の再利用（削除後に同じIDを再登録したいか）は運用で要確認（UNIQUEが効くため）
* “Updateを減らす”方針の中でも、ここは必要最小限のUPDATEが発生する前提

---

### 2) AUTH_ROLE（更新あり）

**役割**
ロールのマスタ。ロールコードと表示名、利用可否（enabled）を管理します。

**主なユースケース**

* 認可：ログイン時にロールをロードして `GrantedAuthority` 化
* 管理：ロールを無効化して、付与されていても効かないようにする（運用による）

**主要カラム**

* `role_code`：ロール識別子（PK）
* `role_name`：表示用名
* `enabled`：ロール自体の有効/無効
* `created_*/updated_*`：監査

**注意点**

* ロール粒度は詳細設計で決定（現状は管理者/その他を最低限想定）

---

### 3) AUTH_ACCOUNT_ROLE（insert/delete中心）

**役割**
アカウントとロールの対応（N:M）を持つ中間テーブル。
「1アカウント複数ロール」仕様に対応しています。

**主なユースケース**

* 管理者がアカウントにロールを追加/削除
* ログイン時にアカウントのロール一覧を取得

**主要カラム**

* `auth_account_id`：対象アカウント
* `role_code`：付与ロール
* `created_at/by`：付与操作の監査

**注意点**

* “更新”は行わず、付与はINSERT、剥奪はDELETEで扱う想定

---

### 4) AUTH_PASSWORD_HISTORY（insert-only）

**役割**
パスワードの変更履歴を保持します。
「有効期限（90日）」「世代（3世代）」「初期登録/管理者リセット/ユーザ変更」の根拠となる履歴です。

**主なユースケース**

* パスワード変更時：新しいハッシュを履歴に追加
* パスワードポリシー（世代禁止）：直近N件のハッシュと一致しないことを判定
* 有効期限：最新の `changed_at` から90日経過なら変更必須

**主要カラム**

* `auth_password_history_id`：PK
* `auth_account_id`：対象アカウント
* `password_hash`：その時点のハッシュ
* `change_type`：`INITIAL_REGISTER / ADMIN_RESET / USER_CHANGE`
* `changed_at`：実際に変更が“適用された”時刻
* `operated_by`：変更操作を行ったユーザ（管理者リセットなら管理者UserId等、初回登録や自動処理ならNULLも可）
* `created_at/by`：履歴レコード登録の監査（遅延登録・移行も考慮して分離）

**注意点**

* `changed_at` と `created_at` を分けているのは、移行/遅延登録でも意味が破綻しないようにするため

---

### 5) AUTH_LOGIN_HISTORY（insert-only）※A案

**役割**
ログイン試行結果の履歴。前回ログイン日時の表示、ロックアウト判定（連続失敗）、運用監査に使います。

**主なユースケース**

* 成功時：`SUCCESS` を記録し、前回成功日時を引く
* 失敗時：`FAILURE` を記録し、直近の連続失敗数を算出
* ロック中：`LOCKED` を記録（※失敗カウント対象外）
* 無効：`DISABLED` を記録

**主要カラム**

* `auth_login_history_id`：PK
* `auth_account_id`：対象アカウント（A案なので NOT NULL）
* `result`：`SUCCESS / FAILURE / LOCKED / DISABLED`
* `login_at`：ログイン試行時刻
* `created_at/by`：監査（イベントリスナーがSYSTEM等で登録する想定）

**注意点（A案の制約）**

* `auth_account_id NOT NULL` のため「存在しない user_id での失敗」や「アカウントが特定できない失敗」は履歴を残せません
  （必要になったらB案＝input_user_idを持つ設計に拡張）

---

### 6) AUTH_ACCOUNT_LOCK_HISTORY（insert-only）

**役割**
ロック/解除のイベント履歴。現在ロック中かどうかは“最新イベント”から判定します（方針A）。

**主なユースケース**

* 連続失敗が閾値超え：`locked=true` のイベントを追加
* 管理者解除：`locked=false` のイベントを追加
* パスワード初期化時：解除イベントを追加（仕様）

**主要カラム**

* `auth_account_lock_history_id`：PK
* `auth_account_id`：対象アカウント
* `locked`：true=ロック、false=解除
* `reason`：理由（例：THRESHOLD_OVER / ADMIN_UNLOCK / ADMIN_RESET など運用でコード化してもよい）
* `occurred_at`：イベント発生時刻
* `operated_by`：操作したユーザ（管理者解除なら管理者UserId、自動ロックならNULL可）
* `created_at/by`：履歴登録の監査

**注意点**

* レースでLOCKイベントが重複しても許容し、判定は「最新」（ORDER BY occurred_at DESC, id DESC）で決める

---

## 業務（業務スキーマ：仮）

### 7) BIZ_USER_PROFILE（仮：ユーザ詳細）

**役割**
業務側で必要な「ユーザの属性情報（氏名、所属、メール等）」を管理する想定の仮テーブル。
認証情報（userId/パスワード/ロック等）はAP基盤に置き、業務情報と分離します。

**主なユースケース**

* 画面表示：ユーザ名・所属などを各画面で表示
* 業務機能：申請・承認など業務データとの紐付け

**主要カラム（例）**

* `biz_user_profile_id`：業務側PK（仮）
* `auth_account_id`：AP基盤の `AUTH_ACCOUNT.auth_account_id` 参照（FK）
* `display_name / email / department / note`：業務属性（仮）
* `created_*/updated_*`：業務側監査（必要に応じて）

**注意点**

* “業務のユーザID”を別に持ちたい場合でも、AP基盤との紐付けは `auth_account_id` を推奨（ID混同を避ける）

---

必要なら、次に「各テーブルを誰がどのタイミングでINSERT/UPDATEするか（イベント/操作のマトリクス表）」も作れます。これがあると実装の抜け漏れが一気に減ります。
