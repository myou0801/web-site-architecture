# 共通ヘッダー 設計書

## 1. 概要

- **コンポーネント名**: 共通ヘッダー
- **適用範囲**: ログイン後に表示される全ての画面（メニュー画面、業務画面、アカウント管理画面など）
- **概要**: 画面上部に常に表示される共通領域。ユーザー情報、前回ログイン日時、ログアウト機能などを提供する。

## 2. 画面レイアウト

ヘッダーは画面の最上部に配置されます。

```
+------------------------------------------------------------------------------+
| ECサイト                                     [ようこそ、山田 太郎 さん]      |
|                                              [前回ログイン: 2025/11/30 10:00] |
|                                              [ログアウト]                      |
+------------------------------------------------------------------------------+
|                                                                              |
|                             (各画面のコンテンツ)                             |
|                                                                              |
+------------------------------------------------------------------------------+
```

## 3. 表示項目一覧

| No. | 項目名 | 説明 | データ取得元 |
|:----|:---|:---|:---|
| 1 | ユーザー名 | ログインしているユーザーの氏名。（氏名がなければユーザーIDなどを表示） | `AuthUserDetails` から取得したユーザー情報 |
| 2 | 前回ログイン日時 | ログインしているユーザーの前回のログイン日時。初回ログイン時は「(初回ログイン)」と表示。 | `AuthUserDetails` の `previousLoggedInAt` プロパティ |
| 3 | ログアウトリンク | クリックするとログアウト処理を実行し、ログイン画面に遷移する。 | (固定リンク) |

## 4. `AuthUserDetails` からの参照方法

業務チームは、Thymeleafなどのビューテンプレートで `principal` オブジェクトを通じてログインユーザー情報にアクセスできます。

### `AuthUserDetails` の拡張

`GEMINI.md`の`3.6 前回ログイン日時`の通り、Spring Securityの `User` クラスを継承した `AuthUserDetails` クラスに、前回ログイン日時を保持するプロパティを追加します。

```java
// com.myou.ec.ecsite.domain.auth.AuthUserDetails (実装イメージ)
public class AuthUserDetails extends org.springframework.security.core.userdetails.User {

    // ... 他のプロパティ (e.g., ユーザー名)

    private final LocalDateTime previousLoggedInAt;

    public AuthUserDetails(/*... 引数 ...*/, LocalDateTime previousLoggedInAt) {
        super(/*... superの引数 ...*/);
        this.previousLoggedInAt = previousLoggedInAt;
    }

    public LocalDateTime getPreviousLoggedInAt() {
        return previousLoggedInAt;
    }
}
```

### Thymeleafでの表示例

```html
<header>
    <span>ECサイト</span>
    <div th:if="${#authorization.expression('isAuthenticated()')}">
        <span>ようこそ、<span th:text="${#authentication.principal.username}"></span> さん</span>
        <br>
        <span>前回ログイン:</span>
        <span th:if="${#authentication.principal.previousLoggedInAt != null}"
              th:text="${#temporals.format(#authentication.principal.previousLoggedInAt, 'yyyy/MM/dd HH:mm')}">
        </span>
        <span th:if="${#authentication.principal.previousLoggedInAt == null}">
            (初回ログイン)
        </span>
        <form th:action="@{/logout}" method="post" style="display:inline;">
            <button type="submit">ログアウト</button>
        </form>
    </div>
</header>
```

## 5. アクション

| No. | アクション名 | 概要 | 成功時の遷移 |
|:----|:---|:---|:---|
| 1 | ログアウト | `POST /logout` を実行し、セッションを無効化する。 | ログイン画面 (`/login?logout`) へリダイレクト |
