# ユーザーアカウント詳細テーブル 設計書

## 1. 目的

AP基盤（認証）の`AUTH_ACCOUNT`テーブルに紐づく、業務的なユーザー属性（氏名、メールアドレス等）を管理するためのテーブルを定義する。
これにより、認証情報と業務情報を関心事として分離する。

## 2. テーブル定義

- **テーブル名:** `USER_ACCOUNT_DETAIL`
- **テーブル説明:** ユーザーアカウント詳細

| カラム名            | 型             | NOT NULL | PK | FK                                | 説明                                           |
|:--------------------|:---------------|:--------:|:--:|:----------------------------------|:-----------------------------------------------|
| `auth_account_id`   | `BIGINT`       |    ○     | ○  | `AUTH_ACCOUNT(auth_account_id)` | 認証アカウントID。主キーであり、外部キーでもある。 |
| `user_name`         | `VARCHAR(100)` |    ○     |    |                                   | ユーザー氏名                                   |
| `email_address`     | `VARCHAR(255)` |    ○     |    |                                   | メールアドレス                                 |
| `created_at`        | `TIMESTAMP`    |    ○     |    |                                   | 作成日時                                       |
| `created_by`        | `VARCHAR(50)`  |    ○     |    |                                   | 作成者のユーザーID                             |
| `updated_at`        | `TIMESTAMP`    |    ○     |    |                                   | 更新日時                                       |
| `updated_by`        | `VARCHAR(50)`  |    ○     |    |                                   | 更新者のユーザーID                             |

## 3. DDL

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
