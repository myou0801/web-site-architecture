package com.myou.ec.ecsite.domain.auth.model.value;

import java.io.Serializable;
import java.util.Objects;

/**
 * 操作者を表す値オブジェクト。
 * DBの created_by / operated_by 列に格納される。
 */
public record Operator(String value) implements Serializable {

    private static final Operator SYSTEM_OPERATOR = new Operator("SYSTEM");

    public Operator {
        Objects.requireNonNull(value, "operatorId must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("operatorId must not be blank");
        }
    }

    /**
     * 指定されたIDの操作者を作成する。
     * @param operatorId 操作者ID
     * @return Operator
     */
    public static Operator of(String operatorId) {
        if (operatorId == null || operatorId.isBlank()){
            return system();
        }
        return new Operator(operatorId);
    }

    /**
     * ユーザIDから操作者を作成する
     * @param userId ユーザID
     * @return Operator
     */
    public static Operator ofUserId(UserId userId){
        if(userId == null) {
            return system();
        }
        return new Operator(userId.value());
    }

    /**
     * システム操作者を表す Operator を返す。
     * @return SYSTEM Operator
     */
    public static Operator system() {
        return SYSTEM_OPERATOR;
    }

    /**
     * この Operator を UserId に変換する。
     * @return 変換された UserId
     */
    public UserId toUserId() {
        return new UserId(this.value);
    }
}
