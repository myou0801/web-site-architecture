# 認証基盤（auth コンテキスト）設計まとめ

本ドキュメントは、`com.myou.ec.ecsite` プロジェクトにおける  
**AP基盤（認証基盤：auth コンテキスト）** のクラス構成・役割を整理した README です。

---

## 1. 全体構成

### 1.1 Gradle モジュール構成

プロジェクトは DDD のレイヤに対応した **4 モジュール構成** とします。

- `domain`          … ドメインモデル／DomainService／Repository インタフェース
- `application`     … ユースケース／AP基盤 sharedService（業務から利用される入口）
- `infrastructure`  … DB／MyBatis／Spring Security 連携など外部技術依存
- `presentation`    … Controller／Security ハンドラ／Web層

### 1.2 パッケージ命名規約

ルートパッケージ：

```text
com.myou.ec.ecsite
````

モジュールとコンテキストを組み合わせて：

```text
com.myou.ec.ecsite.<module>.<context>...
```

* モジュール: `domain` / `application` / `infrastructure` / `presentation`
* コンテキスト: `auth`（認証基盤）, `account`（業務側アカウント管理）など

本 README では **auth コンテキスト（AP基盤）** にフォーカスします。

---

## 2. モジュール依存関係

```text
domain         … 他モジュールに依存しない中核

application    … implementation project(":domain")

infrastructure … implementation project(":domain")

presentation   … implementation project(":application")
```

依存方向：

* `presentation.auth` → `application.auth.sharedservice`
* `application.auth.sharedservice` → `domain.auth.domainservice` & `domain.auth.repository`
* `domain.auth.repository` → `infrastructure.auth.repository`（MyBatis 実装）
* `presentation.account` / `application.account.service` からも
  `application.auth.sharedservice` を利用可能（業務T ⇒ AP基盤の入口）

---

## 3. auth コンテキストのパッケージ構成

### 3.1 domain モジュール（認証ドメイン）

パス: `ecsite/domain/src/main/java`

```text
com.myou.ec.ecsite.domain.auth.model
 ├─ AuthAccount              … 認証アカウント（auth_account）
 ├─ AuthRole              … ロール（auth_role）
 ├─ AuthAccountRole          … アカウント–ロール関連（auth_account_role）
 ├─ LoginHistory          … ログイン履歴（auth_login_history）
 ├─ AccountLockEvent      … LOCK/UNLOCK イベント（auth_account_lock_history）
 ├─ PasswordHistory       … パスワード履歴（auth_password_history）
 └─ value, policy         … UserId, EncodedPassword, ChangeType, PasswordPolicy など

com.myou.ec.ecsite.domain.auth.repository
 ├─ AuthAccountRepository
 ├─ AuthRoleRepository
 ├─ AuthLoginHistoryRepository
 ├─ AuthAccountLockHistoryRepository
 └─ AuthPasswordHistoryRepository

com.myou.ec.ecsite.domain.auth.domainservice
 ├─ AuthPolicyDomainService
 │    ├─ パスワードポリシー判定（5桁以上・英数字・userId と同一禁止）
 │    ├─ パスワード有効期限（90日）判定
 │    ├─ パスワード履歴世代数（3世代）関連のルール
 │    └─ ロック閾値（6回失敗でロック）の取得 等
 ├─ LoginHistoryDomainService
 │    ├─ 前回ログイン日時の取得（SUCCESS の 2 件目）
 │    └─ 直近 SUCCESS or UNLOCK 以降の FAIL 件数カウント
 ├─ AccountLockDomainService
 │    ├─ 現在ロック中かどうかの判定
 │    └─ LOCK / UNLOCK イベントのドメインルール
 └─ PasswordHistoryDomainService
      ├─ 3世代再利用禁止チェック
      ├─ パスワード履歴登録（changeType: INITIAL_REGISTER / ADMIN_RESET / USER_CHANGE）
      └─ 最終パスワード変更日時の取得
```

> **ポイント**
>
> * ドメインのルール（パスワードポリシー／ロック判定／履歴計算）はすべて DomainService に集約。
> * application や presentation は **DomainService にロジックを委譲**する。

---

### 3.2 application モジュール（AP基盤 sharedService）

パス: `ecsite/application/src/main/java`

#### 3.2.1 業務Tから利用される sharedService

```text
com.myou.ec.ecsite.application.auth.sharedservice
 ├─ AuthAccountAdminSharedService
 │    ├─ アカウント一覧／検索
 │    ├─ 認証系ユーザ詳細取得（AUTH_ACCOUNT + ROLE）
 │    ├─ 新規アカウント登録（初期パスワード設定）
 │    ├─ アカウント更新（有効/無効、ロール変更）
 │    ├─ パスワード初期化 + ロック解除
 │    └─ ロック解除のみ
 │
 ├─ AuthAccountContextSharedService
 │    ├─ 現在ログイン中ユーザの auth_account_id 取得
 │    ├─ 現在ログイン中ユーザの user_id 取得
 │    └─ 前回ログイン日時取得（LoginHistoryDomainService 経由）
 │
 ├─ PasswordChangeSharedService
 │    ├─ 自分自身のパスワード変更（/password/change 用）
 │    │    1. 現パスワードの照合
 │    │    2. パスワードポリシー（桁数・英数字・IDとの一致禁止）
 │    │    3. 3世代履歴チェック
 │    │    4. AUTH_ACCOUNT 更新 + パスワード履歴登録
 │    └─ （必要に応じて）管理者によるパスワード変更API
 │
 └─ internal
      ├─ LoginSharedUseCase
      │    ├─ 認証成功時の一連処理
      │    │    - SUCCESS 履歴登録
      │    │    - 前回ログイン日時取得
      │    │    - 初回/初期化後/期限切れ判定
      │    │    - パスワード変更画面 or メニュー画面への遷移判定
      └─ LockControlSharedUseCase
           ├─ 認証失敗時の処理
           │    - FAIL 履歴登録
           │    - 連続失敗回数の算出
           │    - 6 回到達でロックイベント登録
           └─ ロック中ログイン試行時の LOCKED 履歴登録
```

#### 3.2.2 呼び出し関係（イメージ）

* `PasswordChangeSharedService`

    * `AuthAccountRepository`
    * `AuthPolicyDomainService`
    * `PasswordHistoryDomainService`
* `AuthAccountAdminSharedService`

    * `AuthAccountRepository`
    * `AuthRoleRepository`
    * `AccountLockDomainService`
    * `PasswordHistoryDomainService`
* AuthAccountContextSharedService

    * `SecurityContextHolder` から principal を取得
    * `LoginHistoryDomainService` 経由で前回ログイン日時取得

---

### 3.3 infrastructure モジュール（リポジトリ実装・Security 連携）

パス: `ecsite/infrastructure/src/main/java`

```text
com.myou.ec.ecsite.infrastructure.auth.config
 ├─ DataSourceConfig
 ├─ PasswordEncoderConfig              // BCryptPasswordEncoder Bean
 └─ AuthPropertyConfig                 // auth.password.*（初期パスワード / 有効期限等）読込

com.myou.ec.ecsite.infrastructure.auth.repository
 ├─ MybatisAuthAccountRepository              // AuthAccountRepository 実装
 ├─ MybatisAuthRoleRepository
 ├─ MybatisAuthLoginHistoryRepository
 ├─ MybatisAuthAccountLockHistoryRepository
 └─ MybatisAuthPasswordHistoryRepository

com.myou.ec.ecsite.infrastructure.auth.mapper
 ├─ AuthAccountMapper.xml
 ├─ AuthRoleMapper.xml
 ├─ AuthLoginHistoryMapper.xml
 ├─ AuthAccountLockHistoryMapper.xml
 └─ AuthPasswordHistoryMapper.xml

com.myou.ec.ecsite.infrastructure.auth.security
 └─ CustomUserDetailsService              // Spring Security UserDetailsService 実装
```

* Domain 層の Repository インタフェースを MyBatis で実装。
* `CustomUserDetailsService` は `AuthAccountRepository` / `AuthRoleRepository` を用いて
  Spring Security 用の `UserDetails` を組み立てる。

---

### 3.4 presentation モジュール（Controller／Security ハンドラ）

パス: `ecsite/presentation/src/main/java`

```text
com.myou.ec.ecsite.presentation.auth
 ├─ LoginPageController                  // GET /login の画面表示
 ├─ PasswordChangeController             // GET/POST /password/change
 │    └─ application.auth.sharedservice.PasswordChangeSharedService を利用

com.myou.ec.ecsite.presentation.auth.security
 ├─ SecurityConfig
 │    ├─ フォームログイン設定
 │    │    - loginPage("/login")
 │    │    - usernameParameter("userId")
 │    │    - passwordParameter("password")
 │    ├─ URLごとのアクセス制御（管理者ロールのみ /account/** など）
 │    └─ Success/Failure ハンドラの設定
 ├─ CustomAuthenticationSuccessHandler
 │    └─ LoginSharedUseCase を呼び出し
 └─ CustomAuthenticationFailureHandler
      └─ LockControlSharedUseCase を呼び出し
```

* ログイン画面／パスワード変更画面の Controller はここ。
* 画面からは **直接 DomainService を呼ばず**、
  必ず `application.auth.sharedservice` を経由する。

---

## 4. 業務Tとの関係（利用想定）

業務Tは基本的に **「Controller + Service（業務用）」** のみを担当し、
AP基盤の認証機能は sharedService 経由で利用する。

### 4.1 呼び出しイメージ

```text
presentation.account.*Controller
    ↓
application.account.service.* (業務Service)
    ↓
application.auth.sharedservice.* (AuthAccountAdminSharedService / PasswordChangeSharedService / AuthAccountContextSharedService)
```

例：アカウント詳細画面の Service（業務側）

```java
package com.myou.ec.ecsite.application.account.service;

import com.myou.ec.ecsite.application.auth.sharedservice.AuthAccountAdminSharedService;

public class AccountManagementServiceImpl implements AccountManagementService {

    private final AuthAccountAdminSharedService authAccountAdminSharedService;
    private final UserProfileRepository userProfileRepository;

    @Override
    public AccountDetailDto findAccountDetail(long authAccountId) {
        // 認証系情報（ロック状態／ロール等）は AP基盤 sharedService から取得
        AuthAccountDetailDto authDetail =
            authAccountAdminSharedService.findById(authAccountId);

        // 業務情報（氏名／部署など）は業務のリポジトリから取得
        UserProfile profile = userProfileRepository.findById(authAccountId);

        // 両者をマージして画面DTOに変換
        return AccountDetailDto.from(authDetail, profile);
    }
}
```

---

## 5. 主なユースケースと責務分担

### 5.1 ログイン成功時

1. Spring Security による認証成功
2. `CustomAuthenticationSuccessHandler`

    * `LoginSharedUseCase` 呼び出し
3. `LoginSharedUseCase`

    * `LoginHistoryDomainService` で SUCCESS 履歴登録
    * `LoginHistoryDomainService` で前回ログイン日時取得
    * `PasswordHistoryDomainService` + `AuthPolicyDomainService` で

        * 初回 or 管理者初期化直後 or 90日経過の判定
    * `/password/change` または `/menu` への遷移先決定

### 5.2 ログイン失敗時

1. Spring Security による認証失敗
2. `CustomAuthenticationFailureHandler`

    * `LockControlSharedUseCase` 呼び出し
3. `LockControlSharedUseCase`

    * `LoginHistoryDomainService` で FAIL 履歴登録
    * ロック中であれば `LOCKED` 履歴登録
    * 通常失敗であれば連続失敗回数を計算し、6 回以上で

        * `AccountLockDomainService` 経由で LOCK イベント登録

### 5.3 パスワード変更（ユーザ自身）

1. `/password/change` (`presentation.auth.PasswordChangeController`)
2. `application.auth.sharedservice.PasswordChangeSharedService`
3. DomainService 群

    * `AuthPolicyDomainService`：パスワード構文／ID一致のチェック
    * `PasswordHistoryDomainService`：3世代再利用禁止チェック・履歴登録
    * `AuthAccountRepository`：パスワードハッシュ更新

### 5.4 アカウント管理（業務画面）

1. `presentation.account.*Controller`
2. `application.account.service.*`（業務Service）
3. `application.auth.sharedservice.AuthAccountAdminSharedService` 経由で

    * アカウント検索／詳細／ロール変更／ロック解除／パスワード初期化を実行

---

## 6. まとめ

* **auth コンテキスト** は AP基盤側が責任を持つ認証基盤。
* ドメイン層では `domainservice` として認証ロジックを集約。
* AP基盤から業務に提供する API は
  `application.auth.sharedservice.*` の **sharedService** として公開。
* 業務Tは **Controller + 業務Service** だけを担当し、
  sharedService を通じて認証・ロック・パスワード関連機能を利用する。

この README をベースに、各クラスの詳細設計（メソッドシグネチャ／例外設計／DTO 定義）を詰めていけば、そのまま実装フェーズに入れる想定です。

