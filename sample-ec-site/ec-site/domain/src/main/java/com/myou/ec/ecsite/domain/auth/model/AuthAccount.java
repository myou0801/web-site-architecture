package com.myou.ec.ecsite.domain.auth.model;

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

    /** 有効フラグ。false の場合はログイン不可。 */
    private final boolean enabled;

    /** 論理削除フラグ。true の場合は無効ユーザ扱い。 */
    private final boolean deleted;


    // 監査情報
    private final LocalDateTime createdAt;
    private final UserId createdBy;
    private final LocalDateTime updatedAt;
    private final UserId updatedBy;
    private final LocalDateTime deletedAt;
    private final UserId deletedBy;


    /**
     * 永続化層からの再構築などに使うコンストラクタ。
     * newAccount(...) などのファクトリを通して生成するのが基本。
     */
    public AuthAccount(AuthAccountId id,
                       UserId userId,
                       PasswordHash passwordHash,
                       boolean enabled,
                       boolean deleted,
                       LocalDateTime createdAt,
                       UserId createdBy,
                       LocalDateTime updatedAt,
                       UserId updatedBy,
                       LocalDateTime deletedAt,
                       UserId deletedBy) {

        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "encodedPassword must not be null");
        this.enabled = enabled;
        this.deleted = deleted;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = Objects.requireNonNull(createdBy, "createdByUserId must not be null");
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
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
                true,                     // enabled デフォルト true
                false,                    // deleted デフォルト false
                now,
                operator,
                now,
                operator,
                null,
                null                     // versionNo 初期値
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
                this.enabled,
                this.deleted,
                this.createdAt,
                this.createdBy,
                now,
                operator,
                this.deletedAt,
                this.deletedBy
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
                this.enabled,
                this.deleted,
                this.createdAt,
                this.createdBy,
                now,
                operator,
                this.deletedAt,
                this.deletedBy
        );
    }

    /**
     * アカウントを有効にする。
     */
    public AuthAccount enable(LocalDateTime now, UserId operator) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                this.id,
                this.userId,
                this.passwordHash,
                true,
                this.deleted,
                this.createdAt,
                this.createdBy,
                now,
                operator,
                this.deletedAt,
                this.deletedBy
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
                false,
                this.deleted,
                this.createdAt,
                this.createdBy,
                now,
                operator,
                this.deletedAt,
                this.deletedBy
        );
    }

    /**
     * アカウントを論理削除状態にする。
     */
    public AuthAccount markDeleted(LocalDateTime now, UserId operator) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                this.id,
                this.userId,
                this.passwordHash,
                this.enabled,
                true,
                this.createdAt,
                this.createdBy,
                now,
                operator,
                now,
                operator
        );
    }


    /**
     * ログイン可能かどうかの簡易判定（ロック状態は別途 LockStatus で判定）。
     */
    public boolean canLogin() {
        return enabled && !deleted;
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

    public boolean enabled() {
        return enabled;
    }

    public boolean deleted() {
        return deleted;
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

    public LocalDateTime deletedAt() {
        return deletedAt;
    }

    public UserId deletedBy() {
        return deletedBy;
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
               ", enabled=" + enabled +
               ", deleted=" + deleted +
               '}';
    }
}
