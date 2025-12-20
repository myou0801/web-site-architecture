package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AccountStatus;
import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.PasswordHash;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;

import java.util.Objects;

/**
 * 認証アカウント Entity（認証に必要な情報のみを保持）。
 * <p>
 * ユーザの氏名・所属などの業務的な属性は、別ドメイン（業務T側）の
 * アカウント詳細テーブルで管理する前提。
 *
 * @param id            AUTH_ACCOUNT_ID。null の場合は未採番。
 * @param userId        ユーザーID。
 * @param passwordHash  ハッシュ済みパスワード。
 * @param accountStatus アカウント状態。
 */
public record AuthAccount(AuthAccountId id, UserId userId, PasswordHash passwordHash, AccountStatus accountStatus) {

    /**
     * 永続化層からの再構築などに使うコンストラクタ。
     * newAccount(...) などのファクトリを通して生成するのが基本。
     */
    public AuthAccount(AuthAccountId id,
                       UserId userId,
                       PasswordHash passwordHash,
                       AccountStatus accountStatus) {

        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "encodedPassword must not be null");
        this.accountStatus = Objects.requireNonNull(accountStatus, "accountStatus must not be null");
    }

    /**
     * 新規アカウント作成用のファクトリメソッド。
     * まだ ID は採番されていない（id == null）状態で生成する。
     */
    public static AuthAccount newAccount(UserId userId,
                                         PasswordHash passwordHash) {

        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(passwordHash, "encodedPassword must not be null");

        return new AuthAccount(
                null,                     // id (未採番)
                userId,
                passwordHash,
                AccountStatus.ACTIVE     // accountStatus デフォルト ACTIVE
        );
    }

    // ===== ビジネス振る舞い =====

    /**
     * パスワードを変更する。
     * 監査カラム（updatedAt/updatedBy/version）はインフラ層で更新する想定。
     */
    public AuthAccount changePassword(PasswordHash newPassword) {
        Objects.requireNonNull(newPassword, "newPassword must not be null");
        return new AuthAccount(this.id, this.userId, newPassword, this.accountStatus);
    }

    /**
     * ユーザーIDを変更する。
     */
    public AuthAccount changeUserId(UserId newUserId) {
        Objects.requireNonNull(newUserId, "newUserId must not be null");
        return new AuthAccount(this.id, newUserId, this.passwordHash, this.accountStatus);
    }

    /**
     * アカウントを有効にする。
     */
    public AuthAccount activate() {
        return new AuthAccount(this.id, this.userId, this.passwordHash, AccountStatus.ACTIVE);
    }

    /**
     * アカウントを無効にする。
     */
    public AuthAccount disable() {
        return new AuthAccount(this.id, this.userId, this.passwordHash, AccountStatus.DISABLED);
    }

    /**
     * アカウントを論理削除状態にする。
     */
    public AuthAccount markAsDeleted() {
        return new AuthAccount(this.id, this.userId, this.passwordHash, AccountStatus.DELETED);
    }


    /**
     * ログイン可能かどうかの簡易判定（ロック状態は別途 LockStatus で判定）。
     */
    public boolean canLogin() {
        return accountStatus == AccountStatus.ACTIVE;
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
