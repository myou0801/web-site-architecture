# 業務ドメイン - アカウント管理機能 設計書

## 1. 概要

本設計書は、アカウント管理画面におけるユーザー検索機能を実現するために、AP基盤（認証）が提供するコンポーネントを利用しつつ、業務Tが追加で実装したコンポーネントとその連携について記述する。
特に「AP基盤の成果物を修正しない」という制約の下、業務Tの責務範囲でデータアクセス層からアプリケーション層までを構築している。

## 2. アーキテクチャ概要

アカウント管理画面の検索機能は、以下のレイヤーとコンポーネントで構成される。

-   **Presentation層**:
    -   `AccountManagementController`: 画面からのリクエストを受け付け、`AccountAdminQueryService`を呼び出し、結果をViewに渡す。
-   **Application層**:
    -   `AccountAdminQueryService`: `UserAccountDetailRepository`とAP基盤の`AuthAccountRepository`を連携させ、検索条件に合致するアカウント情報を取得し、`AccountSearchResultDTO`にマッピングする。
    -   `AccountSearchResultDTO`: 検索結果を画面に表示するためのDTO。`AuthAccount`（AP基盤）と`UserAccountDetail`（業務ドメイン）の情報を結合して保持する。
-   **Domain層 (業務ドメイン)**:
    -   `UserAccountDetail`: ユーザーの氏名、メールアドレスなどの業務的な詳細情報を保持するエンティティ。`AuthAccountId`と`UserId`（AP基盤由来）も保持する。
    -   `UserAccountDetailRepository`: `UserAccountDetail`エンティティの永続化インターフェース。`userId`と`userName`での検索機能、および`AuthAccountId`での取得機能を提供する。
-   **Infrastructure層 (業務ドメイン)**:
    -   `UserAccountDetailRecord`: `USER_ACCOUNT_DETAIL`テーブルのレコードに対応するクラス。
    -   `UserAccountDetailMapper`: MyBatisのMapperインターフェース。`search`メソッドにより、`USER_ACCOUNT_DETAIL`と`AUTH_ACCOUNT`を結合して検索を行う。
    -   `UserAccountDetailRepositoryImpl`: `UserAccountDetailRepository`インターフェースの実装クラス。`UserAccountDetailMapper`を介してDBアクセスを行い、RecordとDomainエンティティの変換を担当する。

### AP基盤との連携

-   `AccountAdminQueryService`は、AP基盤の`AuthAccountRepository`と`AuthAccountLockHistoryRepository`をDIし、`AuthAccount`の有効/削除状態やロック状態の情報を取得する。
-   AP基盤の`AuthAccountMapper.xml`は変更せず、`AuthAccountRepository`の既存の`findById`メソッドを呼び出すことで`AuthAccount`の情報を取得する。

## 3. 各コンポーネントの詳細

### 3.1. UserAccountDetail (Entity)

-   **ファイル**: `domain/src/main/java/com/myou/ec/ecsite/domain/user/model/UserAccountDetail.java`
-   **概要**: ユーザーの業務的な詳細情報を表現するドメインエンティティ。AP基盤の`AuthAccount`と`authAccountId`で紐づく。
-   **主な属性**:
    -   `authAccountId` (AuthAccountId): 認証アカウントID (AP基盤由来)
    -   `userId` (UserId): ユーザーID (AP基盤由来)
    -   `userName` (String): ユーザー氏名
    -   `emailAddress` (String): メールアドレス
    -   監査カラム (`createdAt`, `createdBy`, `updatedAt`, `updatedBy`)

### 3.2. UserAccountDetailRepository (Interface)

-   **ファイル**: `domain/src/main/java/com/myou/ec/ecsite/domain/user/repository/UserAccountDetailRepository.java`
-   **概要**: `UserAccountDetail`エンティティの永続化に関する契約を定義するインターフェース。
-   **主なメソッド**:
    -   `List<UserAccountDetail> search(String userId, String userName)`: `userId`および`userName`での部分一致検索。
    -   `Optional<UserAccountDetail> findByAuthAccountId(AuthAccountId authAccountId)`: `AuthAccountId`による単一取得。

### 3.3. UserAccountDetailRecord

-   **ファイル**: `infrastructure/src/main/java/com/myou/ec/ecsite/infrastructure/user/record/UserAccountDetailRecord.java`
-   **概要**: `USER_ACCOUNT_DETAIL`テーブルのレコード構造を表現するPOJO。`userId`も含む。

### 3.4. UserAccountDetailMapper (MyBatis Mapper)

-   **インターフェース**: `infrastructure/src/main/java/com/myou/ec/ecsite/infrastructure/user/mapper/UserAccountDetailMapper.java`
-   **XML**: `infrastructure/src/main/resources/com/myou/ec/ecsite/infrastructure/user/mapper/UserAccountDetailMapper.xml`
-   **概要**: `UserAccountDetail`のDBアクセスをMyBatisで実現するMapper。
-   **`search`クエリの詳細**:
    -   `USER_ACCOUNT_DETAIL`テーブルとAP基盤の`AUTH_ACCOUNT`テーブルを`auth_account_id`で`INNER JOIN`する。
    -   `WHERE`句で`AUTH_ACCOUNT.user_id`と`USER_ACCOUNT_DETAIL.user_name`の両方に対し、部分一致（`LIKE CONCAT('%', #{param}, '%')`）検索を動的に適用する。
    -   `AUTH_ACCOUNT.user_id`も検索結果として取得し、`UserAccountDetailRecord.userId`にマッピングする。

### 3.5. UserAccountDetailRepositoryImpl (Repository Implementation)

-   **ファイル**: `infrastructure/src/main/java/com/myou/ec/ecsite/infrastructure/user/repository/UserAccountDetailRepositoryImpl.java`
-   **概要**: `UserAccountDetailRepository`インターフェースの具体的な実装。`UserAccountDetailMapper`をDIし、RecordとDomainエンティティの相互変換を行う。

### 3.6. AccountSearchResultDTO

-   **ファイル**: `application/src/main/java/com/myou/ec/ecsite/application/admin/dto/AccountSearchResultDTO.java`
-   **概要**: アカウント管理画面の検索結果の1行を表現するDTO。`AuthAccount`（`enabled`, `deleted`, `locked`）と`UserAccountDetail`（`userName`, `emailAddress`）の情報を結合して保持する。

### 3.7. AccountAdminQueryService

-   **ファイル**: `application/src/main/java/com/myou/ec/ecsite/application/admin/service/AccountAdminQueryService.java`
-   **概要**: アカウント管理画面の検索ロジックを担当するアプリケーションサービス。
-   **依存関係**: `AuthAccountRepository` (AP基盤), `UserAccountDetailRepository` (業務T), `AuthAccountLockHistoryRepository` (AP基盤)。
-   **`searchAccounts`メソッドの処理フロー**:
    1.  `UserAccountDetailRepository.search(userId, userName)`を呼び出し、`UserAccountDetail`のリストを取得する。
    2.  取得した`UserAccountDetail`のリストから`AuthAccountId`のリストを抽出し、それらを使ってAP基盤の`AuthAccountRepository.findById`をループで呼び出し、`AuthAccount`のマップを構築する。（AP基盤変更不可のため、N+1問題は許容する）
    3.  `UserAccountDetail`と`AuthAccount`（および`AuthAccountLockHistoryRepository`で取得したロック情報）を結合し、`AccountSearchResultDTO`のリストを作成して返す。

## 4. DDL: USER_ACCOUNT_DETAILテーブル

```sql
CREATE TABLE USER_ACCOUNT_DETAIL (
    auth_account_id BIGINT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) NOT NULL,
    PRIMARY KEY (auth_account_id),
    FOREIGN KEY (auth_account_id) REFERENCES AUTH_ACCOUNT (auth_account_id)
);

COMMENT ON TABLE USER_ACCOUNT_DETAIL IS 'ユーザーアカウント詳細';
COMMENT ON COLUMN USER_ACCOUNT_DETAIL.auth_account_id IS '認証アカウントID';
COMMENT ON COLUMN USER_ACCOUNT_DETAIL.user_name IS 'ユーザー氏名';
COMMENT ON COLUMN USER_ACCOUNT_DETAIL.email_address IS 'メールアドレス';
COMMENT ON COLUMN USER_ACCOUNT_DETAIL.created_at IS '作成日時';
COMMENT ON COLUMN USER_ACCOUNT_DETAIL.created_by IS '作成者';
COMMENT ON COLUMN USER_ACCOUNT_DETAIL.updated_at IS '更新日時';
COMMENT ON COLUMN USER_ACCOUNT_DETAIL.updated_by IS '更新者';
```
