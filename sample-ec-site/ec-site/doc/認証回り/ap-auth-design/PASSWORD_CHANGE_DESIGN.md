# パスワード変更（AP基盤）設計書

## 1. 目的
- パスワード変更処理を AP基盤の SharedService として提供する。
- 変更時の入力チェック（ポリシー）・現パスワード照合・世代（履歴）チェックを AP基盤側で行い、業務TのControllerは例外を捕捉して入力エラー表示を行う。

## 2. 変更API（SharedService）
### 2.1 インタフェース
`PasswordChangeSharedService`
- `void changePassword(AuthUserId userId, String currentRawPassword, String newRawPassword);`

※ confirm（new/confirm一致）は presentation（業務T）側で行う。

## 3. パスワードポリシー（確定）
### 3.1 必須条件
- 12文字以上
- 文字種（英大文字/英小文字/数字/記号）のうち **3種類以上**
- 記号は次のみ許可：
  - `#$%()+=?@*[]{}|\`
- 許可しない：
  - `"&';<>`（ただし本設計では「許可文字集合」を固定するため、結果的にこれらは自動的にNG）
  - スペース
  - 半角カナ
  - 全角文字
  - 上記以外のすべての文字

### 3.2 設計上の方針（重要）
- 「禁止文字リスト」を追いかけず、**許可する文字集合**を固定し、それ以外はすべてNGとする。
- これにより、スペース/半角カナ/全角/未許可記号/禁止文字（"&';<>）は一括で排除できる。

### 3.3 許可文字の正規表現（参考）
- 概念：
  - `^[A-Za-z0-9#$%()+=?@*\[\]{}|\\]{12,}$`
- Java文字列：
  - `"^[A-Za-z0-9#$%()+=?@*\\[\\]{}|\\\\]{12,}$"`

## 4. 世代（履歴）チェック
- 3世代（直近3件）に同一パスワードを再利用できない。
- 判定方法：履歴の password_hash に対して `PasswordEncoder.matches(newRaw, oldHash)` を用いる。

## 5. データ更新（トランザクション）
### 5.1 対象テーブル
- AUTH_ACCOUNT（update）
  - password_hash を更新
  - updated_at / updated_by を更新（運用に合わせる）
- AUTH_PASSWORD_HISTORY（insert-only）
  - change_type = USER_CHANGE
  - changed_at / changed_by / created_at / created_by を設定

### 5.2 トランザクション境界
- `PasswordChangeSharedServiceImpl.changePassword` を `@Transactional` とし、
  - 現パスワード照合
  - ポリシーチェック
  - 世代チェック
  - AUTH_ACCOUNT更新
  - AUTH_PASSWORD_HISTORY追加
  を同一トランザクションで実行する。

## 6. 例外設計（業務TのControllerで捕捉）
- `PasswordPolicyViolationException`
  - ポリシー違反（複数の違反を保持）
  - Controllerが violations をフォームエラーに変換して表示する想定
- `CurrentPasswordMismatchException`
  - 現パスワードが一致しない
- `AuthAccountNotFoundException`
  - userId（内部ID）に対応するアカウントが存在しない

## 7. AP基盤 / 業務T 分担
- AP基盤T：本設計のSharedService、ポリシー、Repository、MyBatis Mapper（AUTH_ACCOUNT / AUTH_PASSWORD_HISTORY）
- 業務T：パスワード変更画面、confirmチェック、例外捕捉とエラーメッセージ表示


## 8. HTMLサンプル
``` html
<form th:action="@{/account/password/change}" method="post" th:object="${passwordForm}">
  <div>
    <label for="currentPassword">現在のパスワード</label>
    <input id="currentPassword"
           type="password"
           th:field="*{currentPassword}"
           required
           autocomplete="current-password" />
    <div class="error" th:if="${#fields.hasErrors('currentPassword')}"
         th:errors="*{currentPassword}">error</div>
  </div>

  <div>
    <label for="newPassword">新しいパスワード</label>

    <input id="newPassword"
           type="password"
           th:field="*{newPassword}"
           required
           minlength="12"
           maxlength="72"
           autocomplete="new-password"

           pattern="^[A-Za-z0-9#$%()+=?@*\[\]{}|\\]{12,}$"

           title="12文字以上。使用可能文字：英大/英小/数字と記号 # $ % ( ) + = ? @ * [ ] { } | \ のみ（スペース・全角・半角カナ不可）。" />

    <!-- サーバ側(SharedService)のポリシー違反メッセージを表示 -->
    <div class="error" th:if="${#fields.hasErrors('newPassword')}"
         th:errors="*{newPassword}">error</div>

    <ul>
      <li>12文字以上</li>
      <li>英大文字・英小文字・数字・記号のうち3種類以上（←これはサーバ側で判定）</li>
      <li>記号は #$%()+=?@*[]{}|\ のみ</li>
    </ul>
  </div>

  <div>
    <label for="confirmPassword">新しいパスワード（確認）</label>
    <input id="confirmPassword"
           type="password"
           th:field="*{confirmPassword}"
           required
           minlength="12"
           maxlength="72"
           autocomplete="new-password"
           pattern="^[A-Za-z0-9#$%()+=?@*\[\]{}|\\]{12,}$"
           title="新しいパスワードと同じ条件です。" />

    <div class="error" th:if="${#fields.hasErrors('confirmPassword')}"
         th:errors="*{confirmPassword}">error</div>
  </div>

  <button type="submit">変更</button>
</form>

```

---
以上。
