package com.myou.ec.ecsite.domain.auth.model;

import com.myou.ec.ecsite.domain.auth.model.value.AuthAccountId;
import com.myou.ec.ecsite.domain.auth.model.value.EncodedPassword;
import com.myou.ec.ecsite.domain.auth.model.value.UserId;
import com.myou.ec.ecsite.domain.auth.model.value.RoleCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 認証アカウント Entity（認証に必要な情報のみを保持）。
 *
 * ユーザの氏名・所属などの業務的な属性は、別ドメイン（業務T側）の
 * アカウント詳細テーブルで管理する前提。
 */
public class AuthAccount {

    /** AUTH_ACCOUNT_ID。null の場合は未採番。 */
    private final AuthAccountId id;

    /** ユーザーID。 */
    private UserId userId;

    /** ハッシュ済みパスワード。 */
    private EncodedPassword encodedPassword;

    /** 有効フラグ。false の場合はログイン不可。 */
    private boolean enabled;

    /** 論理削除フラグ。true の場合は無効ユーザ扱い。 */
    private boolean deleted;

    /** 付与されているロール一覧。 */
    private List<RoleCode> roleCodes;

    // 監査情報
    private final LocalDateTime createdAt;
    private final UserId createdByUserId;
    private final LocalDateTime updatedAt;
    private final UserId updatedByUserId;
    private final long versionNo;

    /**
     * 永続化層からの再構築などに使うコンストラクタ。
     * newAccount(...) などのファクトリを通して生成するのが基本。
     */
    public AuthAccount(AuthAccountId id,
                    UserId userId,
                    EncodedPassword encodedPassword,
                    boolean enabled,
                    boolean deleted,
                    List<RoleCode> roleCodes,
                    LocalDateTime createdAt,
                    UserId createdByUserId,
                    LocalDateTime updatedAt,
                    UserId updatedByUserId,
                    long versionNo) {

        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.encodedPassword = Objects.requireNonNull(encodedPassword, "encodedPassword must not be null");
        this.enabled = enabled;
        this.deleted = deleted;
        this.roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId must not be null");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
        this.updatedByUserId = updatedByUserId != null ? updatedByUserId : createdByUserId;
        this.versionNo = versionNo;
    }

    /**
     * 新規アカウント作成用のファクトリメソッド。
     * まだ ID は採番されていない（id == null）状態で生成する。
     */
    public static AuthAccount newAccount(UserId userId,
                                   EncodedPassword encodedPassword,
                                   List<RoleCode> roleCodes,
                                   LocalDateTime now,
                                   UserId operator) {

        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(encodedPassword, "encodedPassword must not be null");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        return new AuthAccount(
                null,                     // id (未採番)
                userId,
                encodedPassword,
                true,                     // enabled デフォルト true
                false,                    // deleted デフォルト false
                roleCodes,
                now,
                operator,
                now,
                operator,
                0L                        // versionNo 初期値
        );
    }

    // ===== ビジネス振る舞い =====

    /**
     * パスワードを変更する。
     * 監査カラム（updatedAt/updatedBy/version）はインフラ層で更新する想定。
     */
    public void changePassword(EncodedPassword newPassword) {
        this.encodedPassword = Objects.requireNonNull(newPassword, "newPassword must not be null");
    }

    /**
     * ユーザーIDを変更する。
     */
    public void changeUserId(UserId newUserId) {
        this.userId = Objects.requireNonNull(newUserId, "newUserId must not be null");
    }

    /**
     * アカウントを有効にする。
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * アカウントを無効にする。
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * アカウントを論理削除状態にする。
     */
    public void markDeleted() {
        this.deleted = true;
    }

    /**
     * ロールを差し替える。
     */
    public void changeRoles(List<RoleCode> newRoles) {
        this.roleCodes = newRoles == null ? List.of() : List.copyOf(newRoles);
    }

    /**
     * ロールを1つ追加する（重複チェックは呼び出し側で必要に応じて行う）。
     */
    public void addRole(RoleCode roleCode) {
        Objects.requireNonNull(roleCode, "roleCode must not be null");
        List<RoleCode> copy = new ArrayList<>(roleCodes);
        copy.add(roleCode);
        this.roleCodes = List.copyOf(copy);
    }

    /**
     * ロールを1つ削除する。
     */
    public void removeRole(RoleCode roleCode) {
        Objects.requireNonNull(roleCode, "roleCode must not be null");
        List<RoleCode> copy = new ArrayList<>(roleCodes);
        copy.remove(roleCode);
        this.roleCodes = List.copyOf(copy);
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

    public EncodedPassword encodedPassword() {
        return encodedPassword;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean deleted() {
        return deleted;
    }

    public List<RoleCode> roleCodes() {
        return roleCodes;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public UserId createdByUserId() {
        return createdByUserId;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public UserId updatedByUserId() {
        return updatedByUserId;
    }

    public long versionNo() {
        return versionNo;
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
               ", roleCodes=" + roleCodes +
               ", versionNo=" + versionNo +
               '}';
    }
}
