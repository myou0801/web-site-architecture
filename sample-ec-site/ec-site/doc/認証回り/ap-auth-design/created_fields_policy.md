# created_at / created_by 設計方針

本ドキュメントは、認証系Webアプリケーションにおける `created_at` と `created_by` の扱いを統一するための設計方針を示す。

## 1. 目的と前提

- **目的**
  - 監査・運用上の「作成時刻」「作成者」を一貫したルールで記録し、実装差異（Mapperごとのブレ）を防ぐ。
  - ドメイン（業務ルール）と永続化（監査メタデータ）を混同しない。

- **前提（本プロジェクトの方針）**
  - 履歴テーブルは insert-only とし、業務事実（発生時刻・操作者）は主に履歴側に残す。
  - `created_at` は「INSERTされた時刻」であり、常に現時刻で記録する。

---

## 2. created_at 方針

### 2.1 方針（決定）
- `created_at` は **DB側で `CURRENT_TIMESTAMP` により自動設定**する。
- アプリケーション（Record/Entity）から `created_at` を明示指定しない。

### 2.2 DDL（推奨）
- PostgreSQL（例）
  - `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
  - （タイムゾーン運用を厳密にする場合は `TIMESTAMPTZ` を検討）

- H2（例）
  - `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`

### 2.3 MyBatis 実装ルール
- INSERT文では `created_at` カラムを **指定しない**。
- INSERT直後に `created_at` を参照する必要がある場合は、以下のいずれかで取得する。
  - INSERT後に SELECT で取得する（推奨：実装一貫性が高い）
  - （PostgreSQL前提の場合）`RETURNING` を利用して取得する

### 2.4 Record/Domain への持たせ方
- `created_at` は **永続化・運用メタデータ**であり、原則として **ドメインエンティティには含めない**。
  - MyBatis Record（DB行表現）には保持してよい。
  - 画面表示・APIレスポンスに必要な場合のみ DTO/ViewModel に載せる。

---

## 3. created_by 方針

### 3.1 方針（決定）
- `created_by` は **アプリケーションで決定してINSERTする**（DBで自動決定しない）。
- 値の意味は「作成操作を実行した主体（操作者）」とする。

### 3.2 値の決定ルール（優先順位）
1. **ログインユーザ**（管理者/一般ユーザ）  
   - Spring Security の認証主体（Principal）から取得した識別子（例：`user_id`）を格納
2. **システム起因の処理**（バッチ、メンテナンス、初期データ投入等）  
   - 固定値 `SYSTEM`（または運用で定めたシステムID）を格納

> 注：`created_by` の粒度（user_id / auth_account_id / 管理者ID 等）は、プロジェクト命名・識別方針に従い統一する。

### 3.3 DDL（推奨）
- `created_by VARCHAR(64) NOT NULL`
  - 長さは運用する主体IDの仕様に合わせて調整する。
  - `NOT NULL` を推奨（主体不明データを作らない）。

### 3.4 MyBatis 実装ルール
- INSERT文で `created_by` を必ず指定する。
- 取得元はアプリケーションの「操作者コンテキスト（Operator）」から取得する。
  - 例：`Operator.userId()` / `Operator.type()` をアプリ層で解決し、Mapperに渡す。

### 3.5 Record/Domain への持たせ方
- `created_by` も原則として **ドメインエンティティには含めない**（運用メタデータ）。
- ただし「作成者によって権限制御する」など **業務ルールの判断材料**になる場合は、ドメイン属性として昇格させる。

---

## 4. 関連ルール（補足）

### 4.1 履歴テーブルとの関係（推奨）
- 履歴（insert-only）では、次を区別して持つと説明責任が明確になる。
  - `occurred_at`：業務事実の発生時刻（アプリ側Clockで決定）
  - `operated_by`：業務事実を発生させた主体（アプリで決定）
  - `created_at` / `created_by`：DB登録のメタ情報（`created_at`はDB、`created_by`はアプリ）

### 4.2 テスト方針
- `created_at` はDBが付与するため、単体テストでは「NULLではない」「期待範囲内（現在時刻±許容差）」程度の検証に留める。
- 固定時刻が必要なテストは、履歴の `occurred_at`（アプリで決定する時刻）を対象にする。

---

## 5. 実装チェックリスト

- [ ] DDLに `created_at DEFAULT CURRENT_TIMESTAMP` がある
- [ ] INSERT SQLで `created_at` を指定していない
- [ ] INSERT SQLで `created_by` を必ず指定している
- [ ] `created_by` の値が「操作者コンテキスト」から一貫して決まる
- [ ] ドメインエンティティに `created_*` を安易に持ち込んでいない（必要時のみ）
