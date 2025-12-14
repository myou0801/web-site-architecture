# 認可（方式A：Role + Permission + Account個別ALLOW/DENY）設計書（基本設計）

本書は、Spring MVC（非Boot）+ Spring Security 6.5 + MyBatis + Thymeleaf の構成において、
**URLアクセス制御**と**画面表示制御**を両立し、さらに **同一ロール内でもユーザごとにアクセス可能画面を変えられる** 認可方式（方式A）を定義する。

- Java: 25
- Spring Framework: 6.2
- Spring Security: 6.5
- DB: PostgreSQL（開発時はH2）

---

## 1. 目的 / 要件
### 1.1 目的
- 画面表示の制御（メニュー/ボタン等）と、URLアクセスの制御（直叩き防止）を一貫したルールで実現する。
- **同一ロール**であっても、**ユーザ単位でアクセス可能な画面（URL）を制御**できるようにする。

### 1.2 達成したいこと（要件）
- URLアクセス制御：権限がないユーザは 403（または権限不足画面）
- 画面表示制御：権限がない場合、対象メニュー/ボタンを非表示
- 同一ロール内でユーザごとの例外設定：ALLOW/DENY による上書き

### 1.3 非目的（この設計では扱わない）
- 行レベル（Row-level）のデータ参照制御（ABAC）
- 所属・時間帯・端末など属性式による動的条件（ABAC）
- 監査ログ（誰がいつ権限変更したか）は created_by/created_at の範囲で担保（詳細な変更履歴は別途）

---

## 2. 方式Aの概要（分類）
- 方式Aは **ABACではない**。
- 基本は **RBAC（Role-Based）**：Role → Permission（権限）を付与し、画面/URLの要否を Permission で判定する。
- 追加で **Account個別のPermission上書き（ALLOW/DENY）** を持つ（RBACの例外機構）。

---

## 3. ドメイン用語・概念
- **Role（ロール）**：ユーザの役割（例：ADMIN / USER）
- **Permission（権限）**：アクセス判定の単位（例：ADMIN_ACCOUNT_VIEW）
- **AccountOverride（個別上書き）**：ユーザ単位の ALLOW / DENY 設定
- **有効権限（Effective Permissions）**：ログイン時に合成された最終的な Permission 集合

---

## 4. 認可ルール（最重要）

### 4.1 権限合成（DENY優先）
ログイン時に以下の合成で Effective Permissions を作成し、Spring Security の `GrantedAuthority` に載せる。

- 役割由来：`rolePerms`（Role → Permission）
- 個別付与：`accountAllows`（Account → Permission, effect=ALLOW）
- 個別禁止：`accountDenies`（Account → Permission, effect=DENY）

**合成式**：
- `effectivePerms = (rolePerms ∪ accountAllows) − accountDenies`

> DENYが最優先：ロールで付与されていても、個別DENYがあればアクセス不可。

### 4.2 判定単位（URL/画面はPermission基準）
- URLアクセス制御：`hasAuthority("PERM_<permissionCode>")`
- 画面表示制御：Thymeleaf で同じ `PERM_...` を利用

### 4.3 命名規約（推奨）
- DBの permission_code は **プレフィックス無し**（例：`ADMIN_ACCOUNT_VIEW`）
- Spring の GrantedAuthority は **PERM_** を付与（例：`PERM_ADMIN_ACCOUNT_VIEW`）
- Role は DB側は `ADMIN` を基本とし、GrantedAuthority は **ROLE_** を付与（例：`ROLE_ADMIN`）

---

## 5. データ設計（AP基盤：認可）

### 5.1 既存（前提）
- `AUTH_ACCOUNT`
- `AUTH_ROLE`
- `AUTH_ACCOUNT_ROLE`（1アカウント複数ロール）

### 5.2 追加（方式Aのために追加）
- `AUTH_PERMISSION`：権限マスタ
- `AUTH_ROLE_PERMISSION`：ロール→権限
- `AUTH_ACCOUNT_PERMISSION`：アカウント→権限（ALLOW/DENY）

### 5.3 テーブル概要
#### AUTH_PERMISSION（権限マスタ）
- 画面/機能を識別する権限コードの一覧
- enabled=false の権限は付与・判定対象外とする

#### AUTH_ROLE_PERMISSION（ロール権限）
- ロールが持つ標準の権限セット
- 原則 INSERT/DELETE で管理（UPDATE最小化方針）

#### AUTH_ACCOUNT_PERMISSION（個別上書き）
- 同一ロール内で個別差分を作るための上書き
- effect=ALLOW/DENY
- 原則 INSERT/DELETE で管理（effect変更は delete+insert）

> 既存の「更新を減らしたい」方針に合わせ、権限付与は insert-only ではないが、UPDATEは避ける運用とする。

---

## 6. 権限解決（ログイン時）設計

### 6.1 実装責務
- `AuthUserDetailsService`（または同等の認証部品）でアカウントを解決した後、
  `GrantedAuthority` を組み立てて `AuthUserDetails` に設定する。

### 6.2 依存Repository（例）
- `AuthAccountRoleRepository#findRolesByAccountId(...)`
- `AuthRolePermissionRepository#findPermissionsByRoles(...)`
- `AuthAccountPermissionRepository#findAllowedPermissions(...)`
- `AuthAccountPermissionRepository#findDeniedPermissions(...)`

### 6.3 合成手順（疑似コード）
1. roles取得（enabledなロールのみ）
2. rolePerms取得（enabledなpermissionのみ）
3. accountAllows / accountDenies 取得（enabledなpermissionのみ）
4. effectivePerms を合成（DENY優先）
5. GrantedAuthorityへ変換
   - roles: `ROLE_<role>`
   - perms: `PERM_<permissionCode>`

> 無効（enabled=false）の Role/Permission は SQL側で除外する（安全・単純化）。

---

## 7. URLアクセス制御（Spring Security）設計

### 7.1 原則
- **URL制御が強制力の本体**。画面表示制御はUXを良くする補助。
- Controller内のif分岐で認可するのは原則禁止（統制・一覧性が崩れるため）。

### 7.2 典型のURL分類（例）
- 認証不要：`/login`, `/css/**`, `/js/**`, `/images/**`
- 管理：`/admin/**`
- 業務：`/biz/**`

### 7.3 URL→Permission割当（設計成果物）
業務Tは画面追加のたび、**URLパターン→Permission** を表（設計書）に追加し、AP基盤Tは SecurityConfig に反映する。

（例）
- `/admin/account/**` → `PERM_ADMIN_ACCOUNT_VIEW`（さらに操作単位で CREATE/UPDATE/DELETE を切っても可）
- `/biz/order/**`      → `PERM_BIZ_ORDER_VIEW`

---

## 8. 画面表示制御（Thymeleaf）設計

### 8.1 原則
- **URL制御と同じPermission**で `sec:authorize` を書く（表示と強制が一致）。

### 8.2 例
- メニュー「アカウント管理」ボタン表示：`hasAuthority('PERM_ADMIN_ACCOUNT_VIEW')`
- 画面内「削除」ボタン表示：`hasAuthority('PERM_ADMIN_ACCOUNT_DELETE')`

---

## 9. 管理（権限付与/変更）ユースケース（概要）
### 9.1 ロール付与
- `AUTH_ACCOUNT_ROLE` の追加/削除で管理
- 1アカウント複数ロール

### 9.2 ロール権限付与
- `AUTH_ROLE_PERMISSION` の追加/削除で管理

### 9.3 ユーザ個別上書き（方式Aの中核）
- `AUTH_ACCOUNT_PERMISSION` に以下を登録する
  - 追加で許可：effect=ALLOW
  - 追加で禁止：effect=DENY（推奨：差分はDENY中心に運用）

**推奨運用**：
- ロールで標準セットを付与し、例外は account_permission の DENY で作る  
  → ロール爆発を防止できる。

---

## 10. 例外時の挙動（403/画面）
- 権限不足（認可NG）の場合：403
- 業務Tは 403画面（共通）に誘導するか、共通テンプレートでメッセージを表示する。

---

## 11. 性能・キャッシュ観点
- ログイン時に roles/perms をまとめて引くため、SQLは以下を推奨
  - rolesはまとめて取得
  - rolePerms はロールコード配列で `IN (...)` 一括取得
  - allow/deny も accountIdで一括取得
- 権限はログインセッション中は固定でよいため、基本は `Authentication` に保持し再計算しない。
- 「権限変更を即時反映したい」要件が出た場合は、再ログイン促し or セッション無効化運用を検討（本書の範囲外）。

---

## 12. テスト観点（最低限）
1. ロールのみで許可：rolePermsに含まれる → OK
2. ロールで許可だが個別DENY：DENY優先 → NG
3. ロールで不許可だが個別ALLOW：ALLOWで追加 → OK
4. ロール不許可・個別DENY：NG
5. 無効ロール（AUTH_ROLE.enabled=false）：rolePermsに出ないこと
6. 無効権限（AUTH_PERMISSION.enabled=false）：effectivePermsに出ないこと
7. Thymeleaf表示とURL制御が一致していること（表示されないURLが直叩きで403になる）

---

## 13. AP基盤T / 業務T 分担（再整理）
### AP基盤T
- 認可テーブル（AUTH_PERMISSION / ROLE_PERMISSION / ACCOUNT_PERMISSION）のDDL提供
- MyBatis Mapper/Repository 実装
- ログイン時の権限解決（GrantedAuthority 組み立て）
- SecurityConfigの共通枠（URL分類、403ハンドリング方針）

### 業務T
- 画面・URLごとのPermission設計（表の作成）
- Thymeleafでの表示制御（menu/button）
- アカウント管理画面での権限付与UI（ロール・個別ALLOW/DENY）

---

## 14. 拡張ポイント
- 操作単位のPermission（VIEW/CREATE/UPDATE/DELETE）を段階的に追加可能
- 将来：ABAC（属性条件）や行レベル制御が必要になった場合は別設計として追加（本方式Aの上に重ねる）

---

以上。
