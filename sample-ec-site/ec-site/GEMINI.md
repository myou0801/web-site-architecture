# GEMINI.md

このドキュメントは、本スレッドで合意した **AP基盤（認証）設計** を、実装担当（GEMINI CLI利用）へ共有するためのメモです。  
対象は **Spring MVC（Bootなし）+ Spring Security + MyBatis + Thymeleaf** の Webアプリにおける **認証・認可（ロール）・パスワード運用・ロックアウト・履歴** です。

---

## 0. 前提・技術スタック

- Java: **25**
- Spring Framework: **6.2**
- Spring Security: **6.5**
- ORM: **MyBatis**
- DB: **PostgreSQL（本番） / H2（開発）**
- View: Thymeleaf（※将来的に変更される可能性はあるが、本ドキュメントの範囲は認証AP基盤）
- （重要）Spring Bootは使用しないとされていたが、実装の効率性からSpring Boot Starterを利用する方針に変更された。
- Gradle **9.2.1**

---

## 1. 体制（AP基盤T / 業務Tの分担）

### AP基盤T
- 認証系の共通部品（Domain / Infrastructure / SharedService / Security関連部品）を提供
- DB（AP基盤側）のテーブル設計・実装（MyBatis含む）
- ログイン成功/失敗の履歴登録（イベントリスナー）
- ロック判定・ロックイベント記録（イベント/履歴）
- パスワード変更・ポリシーチェック（SharedService内で実施、エラーは例外で返す）

### 業務T
- Controller と Service を担当（ServiceからAP基盤SharedServiceを呼ぶ）
- 画面（ログイン、メニュー、業務画面、アカウント管理画面）実装
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
- `AuthAccountDetails` (Spring Securityの `UserDetails` 実装) は `domain` ではなく `application` モジュール (`com.myou.ec.ecsite.application.auth.security`) に実装されている。

---

## 3. 認証・認可の仕様（確定事項）

### 3.1 ログイン
- 入力は **ユーザID（UserId）+ パスワード**
- 認証成功でメニューへ（ただし後述の「パスワード変更必須」の場合は変更画面へ誘導）

### 3.2 ロール
- 仕様変更により **1アカウント複数ロール** を許容
- DBは中間テーブルで管理（N:M）

> ドメインの `AuthAccount` にロールを内包しない方針（認証の核に責務を閉じる）。  
> ロールは `AuthAccountRoleRepository` 等で取得し、`AuthUserDetails` の `GrantedAuthority` に反映する。

### 3.3 パスワードポリシー
- 最小桁数: **5**
- 文字種: **英数字**
- **UserId とパスワードの完全一致は禁止**
- 有効期限: **90日**
- 履歴世代: **3世代**（直近3回と同じパスワード禁止）
- ロールによりUserId規約が異なる可能性あり（詳細設計で決定）

補足:
- パスワード変更画面のレイアウト上はパスワードポリシーは表示しないが、設計ドキュメントにはポリシーを明記する。

### 3.4 パスワード初期化
- 管理者ロールがアカウント管理で実施
- 初期パスワードは固定: **password123**（後で調整可）
- 初期化後、次回ログイン成功時は **必ずパスワード変更画面へ**

### 3.5 アカウントロック
- **6回失敗でロック**
- ロック解除は管理者がアカウント管理で実施
- **パスワード初期化するとロックも解除**
- ロック中のログインは履歴に残す:
  - `AUTH_LOGIN_HISTORY.result = 'LOCKED'`
  - ただし **失敗カウントには含めない**

### 3.6 前回ログイン日時
- ログイン後に前回ログイン日時を画面共通ヘッダに表示
- Spring Security の `User` を継承した `AuthUserDetails` に「前回ログイン日時」を保持させ、業務Tが参照できるようにする

---

## 4. 設計方針（重要）

### 4.1 UPDATEを減らす（履歴主導）
- 可能な限り **履歴（insert-only）** を積む
- ただし、**AUTH_ACCOUNT / AUTH_ROLE は更新が必要**（enabled/削除/パスワードハッシュなど）

### 4.2 同時実行（レース）・ロックイベント重複
- 方針A（割り切り）:
  - LOCKイベントの重複は許容
  - 現在状態は「最新イベント」で判断（`ORDER BY occurred_at DESC, id DESC LIMIT 1`）

### 4.3 トランザクション境界
- 原則、業務Tの Service に `@Transactional`
- ただし、業務Tから呼ばれない入口などは SharedService 側に `@Transactional` を付けてもよい（入口境界を明確にする）

### 4.4 アカウント管理画面の検索機能について

- `AUTH_ACCOUNT` (AP基盤) と `USER_ACCOUNT_DETAIL` (業務T) を結合した検索機能は、AP基盤側の`AuthAccountMapper.xml`を修正せず、業務Tの責務において実装する。
- 具体的には、業務Tの`UserAccountDetailMapper.xml`内で`AUTH_ACCOUNT`との結合クエリを記述し、`userId`と`userName`での部分一致検索を可能にする。

---

## 5. DB設計（AP基盤スキーマ）

### 5.1 監査列ルール（確定）
- 原則: **全テーブルに `created_at` + `created_by` （NOT NULL）**
- 更新があるテーブルのみ: **`updated_at` + `updated_by` （NOT NULL）**
- 履歴系（insert-only）は `updated_*` を持たない
- `created_by` は `"SYSTEM"` 等の固定値も可（まず実装簡略化。必要なら操作者伝搬を後で追加）

### 5.2 テーブル一覧（最新版）
- `AUTH_ACCOUNT`（更新あり）
- `AUTH_ROLE`（更新あり）
- `AUTH_ACCOUNT_ROLE`（insert/delete中心）
- `AUTH_PASSWORD_HISTORY`（insert-only）
- `AUTH_LOGIN_HISTORY`（insert-only）※A案
- `AUTH_ACCOUNT_LOCK_HISTORY`（insert-only）

補足:
- `AUTH_LOGIN_HISTORY` は **A案**:
  - `auth_account_id` は NOT NULL（存在するアカウントに紐づけて記録）
  - 不存在UserId入力などは履歴化しない（または別途検討）

---

## 6. 認証イベント処理方針（Spring Security）

- ログイン成功:
  - `AuthenticationSuccessEvent` リスナーで `AUTH_LOGIN_HISTORY(SUCCESS)` をinsert
  - 前回ログイン日時の組み立てに使う
- ログイン失敗:
  - `AbstractAuthenticationFailureEvent` 由来のイベントで `AUTH_LOGIN_HISTORY(FAILURE)` をinsert
  - 失敗回数・連続判定は履歴から算出
- ロック中:
  - `AUTH_LOGIN_HISTORY(LOCKED)` をinsert（失敗カウント対象外）
- 無効（DISABLED）:
  - `AUTH_LOGIN_HISTORY(DISABLED)` をinsertする条件を別途整理し、その条件で記録

※成功/失敗リスナーで RequestContextHolder 経由の取得は極力避ける方針。  
ただし、失敗イベントから入力UserIdが確実に取れないケースがあるため、最終的に「usernameパラメータ名」前提で取得する結論で進める（入力パラメータ名を固定する）。

---

## 7. パスワード変更必須判定の配置

- 履歴登録（成功/失敗）はイベントリスナーで行う
- **パスワード変更必須チェック**は以下を採用:
  - Handler または Interceptor で実施（セッションにフラグを置かない方針）
  - 無限ループ回避のための除外URLは外部設定化
- `AuthAuthenticationSuccessHandler` は `SavedRequestAwareAuthenticationSuccessHandler` を extends し、
  - `defaultSuccessUrl` を SecurityConfig から設定する

---

## 8. MyBatis / Infrastructure 実装ルール

- TypeHandlerは使わない
- DB ↔ Domain 変換は **RepositoryImpl** が担う
- レコードは `*Record`（`DbRecord` という命名は使用しない）
- Repository実装クラス名: `XxxRepositoryImpl`（`MybatisXxx` は使わない）
- MyBatis:
  - Mapper interface + XML を採用
  - `resources/com/myou/.../mapper/*.xml` に配置

---

## 9. 実装物（現時点）

- DDL（最新版）に合わせた infrastructure 実装の更新版を作成済み:
  - Record / Mapper / XML / RepositoryImpl 一式
  - `AUTH_LOGIN_HISTORY` はA案（login_byは持たない）
  - `AUTH_PASSWORD_HISTORY` は `operated_by` を使用（`changed_by` ではなく）

※履歴系の `created_by` / `operated_by` は現状 `"SYSTEM"` や `null` に簡略化している箇所がある。  
運用で必須になれば、SharedServiceの引数に操作者（UserId）を追加して伝搬する。

---

## 10. GEMINI CLI での実装方針（プロンプト用メモ）

### コード生成時に必ず守らせること
- ルート: `com.myou.ec.ecsite`
- modules: presentation/application/domain/infrastructure
- 認証サブドメイン: `...<layer>.auth`
- MyBatisは interface + XML（アノテーションSQLは使わない）
- Recordは `toDomain()/fromDomain()`（または `forInsert()`）で変換
- RepositoryImplが変換責務を持つ（TypeHandler禁止）
- 監査列ルール（created_*必須、更新のみupdated_*）
- 時刻は `Clock` をDIし `LocalDateTime.now(clock)` を使用

### 生成の粒度（おすすめ）
1. domain（Value/Entity/Exception/Policy）→先に固定
2. infrastructure（Record/Mapper/XML/RepositoryImpl）
3. application（SharedService / UseCase）
4. presentation（Handler / Interceptor / Listener / SecurityConfig）

---

## 11. 次のTODO（実装作業の残り）
- application 層:
  - パスワード変更必須判定のユースケース化（期限/初期化後/変更後日数）
- presentation 層:
  - success/failure event listener
  - password change required interceptor
  - security config（defaultSuccessUrl / 除外URL外部化）
- 業務T向けI/F:
  - Controllerで例外捕捉 → 画面表示用エラーメッセージ変換（パスワード違反等）

---

## 12. AP基盤Tの設計情報
- doc/認証回り/ap-auth-design

---

## 13. 業務Tの設計情報
- doc/業務ドメイン

以上。