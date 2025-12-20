# CurrentUserProvider / Operator 設計方針（配置：B案＝presentation/web）

## 目的
- DB へ登録する `created_by` / `operated_by` 等の「操作者」を、ユースケース（Application Service）から Repository へ明示的に受け渡す。
- Spring Security 依存（SecurityContext）を **presentation/web 層に閉じ込め**、domain/application 層を汚さない。

---

## レイヤ配置（合意事項）

### domain
- `Operator`（値オブジェクト）
  - 監査・操作者の識別子を表す。
  - Spring 依存は持たない。

### application
- `CurrentUserProvider`（ポート / インタフェース）
  - ユースケースが「現在の操作者」を取得するための抽象。
  - 実装は持たず、DI で注入されることを前提とする。

### presentation/web
- `SpringSecurityCurrentUserProvider`（`CurrentUserProvider` 実装）
  - `SecurityContextHolder` から principal を解決し、`Operator` を生成して返す。
  - Spring Security 依存はここに限定する。

---

## 依存関係（方向）
- `presentation/web` → `application` → `domain`
- `presentation/web` → `domain`（`Operator` 生成のため）
- `application` は `presentation/web` を参照しない（逆参照しない）

---

## クラス構成（例）

### domain
- `com.example.auth.domain.audit.Operator`

### application
- `com.example.auth.application.security.CurrentUserProvider`

### presentation/web
- `com.example.auth.web.security.SpringSecurityCurrentUserProvider`
- `com.example.auth.web.config.SecurityAdapterConfig`（Bean 定義）

---

## 仕様

### Operator
- `Operator.of(String operatorId)`：操作者IDを生成（空白禁止）
- `Operator.system()`：バッチ・初期化等の非ログイン実行向け（必要な場合のみ使用）

### CurrentUserProvider
- `Optional<Operator> current()`：取得できない場合 empty
- `Operator requireCurrent()`：取得できない場合例外（Webの通常ユースケース向け）
- `Operator currentOrSystem()`：取得できない場合 SYSTEM（バッチ等向け）

---

## SpringSecurityCurrentUserProvider（web層）の解決ルール
`Authentication` から操作者IDを以下の優先順で解決する。

1. principal が独自 `AuthPrincipal` の場合：`getUserId()` を使用（推奨）
2. principal が `UserDetails` の場合：`getUsername()` を使用
3. principal が `String` の場合：その値を使用（"anonymousUser" は除外）
4. 最後に `Authentication#getName()` を使用

匿名認証・未認証の場合は `Optional.empty()` を返す。

---

## DI（Bean 提供）方針
- web 層で `@Configuration` を用意し、`CurrentUserProvider` の Bean として公開する。
- application 層の Service は `CurrentUserProvider` をコンストラクタインジェクションで受け取る。

例：
- `@Bean CurrentUserProvider currentUserProvider() { return new SpringSecurityCurrentUserProvider(); }`

---

## Service / Repository の責務分担
- Application Service：
  - `CurrentUserProvider` から `Operator` を取得する。
  - Repository 呼び出しの引数として `Operator` を必ず渡す（Controllerからは渡さない）。
- Repository：
  - `Operator.value()` を `created_by` / `operated_by` に格納する。
  - SecurityContext を参照しない。

---

## 注意点（運用・テスト）
- Controller から operator を受け取る設計は不可（なりすまし・改ざん余地）。
- バッチ実行がある場合は、web とは別の `CurrentUserProvider` 実装（例：`BatchCurrentUserProvider`）を用意するか、
  ジョブ実行ユーザを Service へ別途渡す設計を採る。
- テストでは `CurrentUserProvider` をモックし、任意の `Operator` を返すようにする。

---
