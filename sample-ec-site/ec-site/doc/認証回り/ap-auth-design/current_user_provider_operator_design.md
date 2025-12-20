# CurrentUserProvider / Operator 設計方針（暫定）

## 目的
- DB書き込み時に `created_by` / `operated_by` を一貫して設定する。
- Spring Security（`SecurityContext`）依存を **Application/Domain に持ち込まず**、Infrastructure に閉じ込める。
- Repository は `SecurityContext` に依存せず、呼び出し元（Application Service）が操作者を明示的に渡す。

---

## レイヤ配置（採用方針）

### Domain
- `Operator`（値オブジェクト）
  - **永続化メタデータそのものを Domain Entity に保持しない**方針は維持しつつ、
    「操作者ID」の型として Domain に定義する（インフラ非依存の純粋な値型）。

### Application
- `CurrentUserProvider`（ポート / インタフェース）
  - ユースケース（Application Service）が「現在の操作者」を取得するためのインタフェース。
  - `SecurityContext` を知らない。

### Infrastructure
- `SpringSecurityCurrentUserProvider`（ポート実装）
  - `SecurityContextHolder` から principal を読み取り、`Operator` に変換して返す。
  - principal の実体（`AuthPrincipal` / `UserDetails` / `String`）差分を吸収する。

---

## 依存関係（方向）
- `application` → `domain`
- `infrastructure` → `application` → `domain`

※ `domain` / `application` は Spring Security に依存しない。

---

## 主要コンポーネント

### Operator（domain）
- 役割：操作者IDの型・妥当性（null/空文字）保証
- 代表API：
  - `Operator.of(String operatorId)`
  - `Operator.system()`（バッチ等の非ログイン実行向け）
  - `String value()`

### CurrentUserProvider（application）
- 役割：ユースケースで操作者を取得するためのポート
- 代表API：
  - `Optional<Operator> current()`
  - `Operator requireCurrent()`（取得できない場合は例外）
  - `Operator currentOrSystem()`（取得できない場合は SYSTEM）

### SpringSecurityCurrentUserProvider（infrastructure）
- 役割：SecurityContext から操作者を抽出
- 抽出順（推奨）：
  1. `principal instanceof AuthPrincipal` → `getUserId()`
  2. `principal instanceof UserDetails` → `getUsername()`
  3. `principal instanceof String` → 文字列（"anonymousUser" 除外）
  4. 最後に `Authentication#getName()`

---

## ユースケースでの利用規約（必須）

### 1) Controller は operator を受け取らない
- なりすまし防止のため、Controller から `createdBy` 等を入力させない。
- Application Service 内で `CurrentUserProvider` から取得する。

### 2) Repository の引数として operator を渡す
- Repository は `SecurityContext` を参照しない。
- 例：
  - `repo.insert(account, operator)`
  - `statusHistoryRepo.insert(id, from, to, operator)`

### 3) created_by / operated_by の格納方針
- DB列（`created_by` / `operated_by`）には `Operator.value()` を格納する。
- 値は原則 **user_id** を採用（運用での可読性が高い）。
  - 不変ID（auth_account_id）を採用したい場合は別途検討（表示・検索性とのトレードオフ）。

---

## 例外・非ログイン実行（バッチ等）
- ログイン必須のユースケース：`requireCurrent()` を使用し、取得できない場合は異常扱い。
- バッチ／初期化：`currentOrSystem()` を使用するか、ジョブ実行ユーザを別経路で渡す。

---

## テスト方針
- Application Service の単体テストでは、`CurrentUserProvider` をモックし `Operator.of("test")` を返す。
- Infrastructure の `SpringSecurityCurrentUserProvider` は、`SecurityContextHolder` をセットした上での結合テストで確認する。

---

## 実装テンプレ（ファイル配置例）
```
domain/
  com.example.auth.domain.audit/Operator.java

application/
  com.example.auth.application.security/CurrentUserProvider.java

infrastructure/
  com.example.auth.infrastructure.security/SpringSecurityCurrentUserProvider.java
  com.example.auth.infrastructure.security/AuthPrincipal.java  (必要に応じて)
```

---

## 運用上の注意
- 監査・追跡が不要な場合でも、`created_by` は障害調査・問合せ対応で有用となることが多い。
- `Operator.system()` の利用箇所は限定し、どの処理が SYSTEM 実行か分かるようにする。
