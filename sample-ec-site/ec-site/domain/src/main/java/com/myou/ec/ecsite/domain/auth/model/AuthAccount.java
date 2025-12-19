package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AccountStatus;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 認証アカウント Entity（認証に必要な情報のみを保持）。
 * <p>
 * ユーザの氏名・所属などの業務的な属性は、別ドメイン（業務T側）の
 * アカウント詳細テーブルで管理する前提。
 */
public class AuthAccount {

    /** AUTH_ACCOUNT_ID。null の場合は未採番。 */
    private final AuthAccountId id;

    /** ユーザーID。 */
    private final UserId userId;

    /** ハッシュ済みパスワード。 */
    private final PasswordHash passwordHash;

    /** アカウント状態。 */
    private final AccountStatus accountStatus;


    // 監査情報
    private final LocalDateTime createdAt;
    private final UserId createdBy;
    private final LocalDateTime updatedAt;
    private final UserId updatedBy;


    /**
     * 永続化層からの再構築などに使うコンストラクタ。
     * newAccount(...) などのファクトリを通して生成するのが基本。
     */
    public AuthAccount(AuthAccountId id,
                       UserId userId,
                       PasswordHash passwordHash,
                       AccountStatus accountStatus,
                       LocalDateTime createdAt,
                       UserId createdBy,
                       LocalDateTime updatedAt,
                       UserId updatedBy) {

        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "encodedPassword must not be null");
        this.accountStatus = Objects.requireNonNull(accountStatus, "accountStatus must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdByUserId must not be null");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
        this.updatedBy = updatedBy != null ? updatedBy : createdBy;
    }

    /**
     * 新規アカウント作成用のファクトリメソッド。
     * まだ ID は採番されていない（id == null）状態で生成する。
     */
    public static AuthAccount newAccount(UserId userId,
                                   PasswordHash passwordHash,
                                   LocalDateTime now,
                                   UserId operator) {

        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(passwordHash, "encodedPassword must not be null");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                null,                     // id (未採番)
                userId,
                passwordHash,
                AccountStatus.ACTIVE,     // accountStatus デフォルト ACTIVE
                now,
                operator,
                now,
                operator
        );
    }

    // ===== ビジネス振る舞い =====

    /**
     * パスワードを変更する。
     * 監査カラム（updatedAt/updatedBy/version）はインフラ層で更新する想定。
     */
    public AuthAccount changePassword(PasswordHash newPassword, LocalDateTime now, UserId operator) {
        Objects.requireNonNull(newPassword, "newPassword must not be null");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                this.id,
                this.userId,
                newPassword,
                this.accountStatus,
                this.createdAt,
                this.createdBy,
                now,
                operator
        );
    }

    /**
     * ユーザーIDを変更する。
     */
    public AuthAccount changeUserId(UserId newUserId, LocalDateTime now, UserId operator) {
        Objects.requireNonNull(newUserId, "newUserId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                this.id,
                newUserId,
                this.passwordHash,
                this.accountStatus,
                this.createdAt,
                this.createdBy,
                now,
                operator
        );
    }

    /**
     * アカウントを有効にする。
     */
    public AuthAccount activate(LocalDateTime now, UserId operator) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                this.id,
                this.userId,
                this.passwordHash,
                AccountStatus.ACTIVE,
                this.createdAt,
                this.createdBy,
                now,
                operator
        );
    }

    /**
     * アカウントを無効にする。
     */
    public AuthAccount disable(LocalDateTime now, UserId operator) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                this.id,
                this.userId,
                this.passwordHash,
                AccountStatus.DISABLED,
                this.createdAt,
                this.createdBy,
                now,
                operator
        );
    }

    /**
     * アカウントを論理削除状態にする。
     */
    public AuthAccount markAsDeleted(LocalDateTime now, UserId operator) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                this.id,
                this.userId,
                this.passwordHash,
                AccountStatus.DELETED,
                this.createdAt,
                this.createdBy,
                now,
                operator
        );
    }


    /**
     * ログイン可能かどうかの簡易判定（ロック状態は別途 LockStatus で判定）。
     */
    public boolean canLogin() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    // ===== getter =====

    public AuthAccountId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public AccountStatus accountStatus() {
        return accountStatus;
    }


    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdBy() {
        return createdBy;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public UserId updatedBy() {
        return updatedBy;
    }


    // ===== equals / hashCode / toString =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthAccount authAccount)) return false;
        // 永続化後は ID で等価判定。未採番同士はインスタンス等価のみ。
        return id != null && id.equals(authAccount.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "AuthAccount{" +
               "id=" + id +
               ", userId=" + userId +
               ", accountStatus=" + accountStatus +
               '}';
    }
}
