# GEMINI.md

このドキュメントは、本スレッドで合意した **AP基盤（認証）設計** を、実装担当（GEMINI CLI利用）へ共有するためのメモです。  
対象は **Spring MVC + Spring Security + MyBatis + Thymeleaf** の Webアプリにおける **認証・認可（ロール）・パスワード運用・ロックアウト・履歴・アカウント状態管理・検索（sharedService）** です。

---

## 0. 前提・技術スタック

- Java: **25**
- Spring Framework: **6.2**
- Spring Security: **6.5**
- ORM: **MyBatis**
- DB: **PostgreSQL（本番） / H2（開発）**
- View: Thymeleaf
- **Spring Boot Starterを利用**する方針
- Build: Gradle **9.2.1**

### 0.1 日時・タイムゾーン
- DB日時型：**TIMESTAMP（timezoneなし）**
- 運用タイムゾーン：**JST（Asia/Tokyo）**
- Java側日時型：**LocalDateTime**
- `created_at` は DB の `DEFAULT CURRENT_TIMESTAMP`（TIMESTAMP）で付与する（アプリから値を渡さない）

---

## 1. 体制（AP基盤T / 業務Tの分担）

### AP基盤T
- 認証系の部品（Domain / Infrastructure / SharedService / Security関連部品）を作成
- DB（AP基盤側）のテーブル設計・実装（MyBatis含む）
- ログイン成功/失敗の履歴登録（イベントリスナー）
- ロック判定・ロックイベント記録（イベント/履歴）
- パスワード変更・ポリシーチェック（SharedService内で実施、エラーは例外で返す）
- 認証回りの画面（ログイン、ログイン失敗、パスワード変更、パスワード変更完了）の実装（Controllerも含める）
- 業務Tへ提供する **sharedService（Command/Query）** を作成する

### 業務T
- 画面（メニュー、業務画面、アカウント管理画面）実装
- アカウント管理系のアカウント登録などはServiceクラスからAP基盤のSharedServiceを呼び出す
- 共通ヘッダへの前回ログイン日時表示（AP基盤のUserDetails経由で利用）

---

## 2. モジュール/パッケージ構成（DDD）

Gradle multi-module（5層）:
- `boot`
- `presentation`
- `application`
- `domain`
- `infrastructure`

ルートパッケージ:
- `com.myou.ec.ecsite`

認証サブドメイン（例）:
- `com.myou.ec.ecsite.<layer>.auth`

命名方針（重要）:
- 画面項目の「ユーザID」: **UserId**
- 内部不変ID: **AuthAccountId**
- これらは混同しない（分離する）

補足:
- `AuthAccountDetails`（Spring Securityの `UserDetails` 実装）は `domain` ではなく `presentation` モジュールに実装する
  - `com.myou.ec.ecsite.presentation.auth.security`

---

## 10. GEMINI CLI での実装方針（プロンプト用メモ）

### コード生成時に必ず守らせること
- ルート: `com.myou.ec.ecsite`
- modules: boot/presentation/application/domain/infrastructure
- 認証サブドメイン: `...<layer>.auth`
- MyBatisは interface + XML（アノテーションSQLは使わない）
- Recordは `toDomain()/fromDomain()`（または `forInsert()`）で変換
- RepositoryImplが変換責務を持つ（TypeHandler禁止）
- 監査列ルール（created_*必須、更新のみupdated_*）
- Java日時は `LocalDateTime`（JST運用）
- `created_at` は DB 付与（SQLで `DEFAULT CURRENT_TIMESTAMP`、アプリは値を渡さない）
- アプリで発生させる時刻（`occurred_at` 等）は `Clock` をDIし `LocalDateTime.now(clock)` を使用

### 生成の粒度（おすすめ）
1. domain（Value/Entity/Exception/Policy）→先に固定
2. infrastructure（Record/Mapper/XML/RepositoryImpl）
3. application（SharedService / UseCase / CurrentUserProvider）
4. presentation（Handler / Interceptor / Listener / SecurityConfig）

---

## 12. AP基盤Tの設計情報
- doc/AP基盤

---

## 13. 業務Tの設計情報
- doc/業務ドメイン

以上。