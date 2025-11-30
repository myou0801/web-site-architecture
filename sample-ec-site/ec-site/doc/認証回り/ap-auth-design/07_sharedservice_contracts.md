# sharedService（I/F・実装テンプレ・Validation契約）

---

## 1. sharedService I/F（確定）

### PasswordChangeSharedService
```java
public interface PasswordChangeSharedService {
    PasswordChangeRequirement requirementOf(AuthAccountId accountId);
    void changePassword(AuthAccountId accountId, String currentRawPassword, String newRawPassword);
}
```

### AuthAccountAdminSharedService
```java
public interface AuthAccountAdminSharedService {
    AuthAccountId registerAccount(UserId newUserId, java.util.Set<RoleCode> roles, UserId operator);
    void resetPassword(AuthAccountId targetAccountId, UserId operator);
    void unlock(AuthAccountId targetAccountId, UserId operator);
    void disableAccount(AuthAccountId targetAccountId, UserId operator);
    void enableAccount(AuthAccountId targetAccountId, UserId operator);
    void addRole(AuthAccountId targetAccountId, RoleCode role, UserId operator);
    void removeRole(AuthAccountId targetAccountId, RoleCode role, UserId operator);
    void deleteAccount(AuthAccountId targetAccountId, UserId operator);
}
```

---

## 2. Validation契約（AP基盤→業務T）

- field名は業務Tフォームのプロパティ名に一致させる
- messageKeyは messages.properties に定義
- args は `{0},{1}…` に差し込む想定

フィールド名（固定）：
- login：`userId`, `password`
- password-change：`currentPassword`, `newPassword`, `confirmPassword`（confirmは業務T）
- admin register：`userId`, `roles`

最低限の messageKey：
- `auth.login.failed`
- `auth.login.locked`
- `auth.login.disabled`
- `auth.password.current.invalid`
- `auth.password.new.minLength`
- `auth.password.new.alphanumeric`
- `auth.password.new.sameAsUserId`
- `auth.password.new.reuseNotAllowed`
- `auth.role.required`
- `auth.role.notFound`
- `auth.role.disabled`
- `auth.account.userId.duplicate`
- `auth.account.role.duplicate`
- `auth.account.notFound`
- `auth.account.deleted`
