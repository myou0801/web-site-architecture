package com.myou.ec.ecsite.application.auth.provider;

import com.myou.ec.ecsite.domain.auth.model.value.Operator;

import java.util.Optional;

public interface CurrentUserProvider {

    /**
     * 現在の操作者（ログイン中ユーザ）を返す。取得できない場合は empty。
     */
    Optional<Operator> current();

    /**
     * 現在の操作者（ログイン中ユーザ）を必須として取得する。
     * 取得できない場合は例外。
     */
    default Operator requireCurrent() {
        return current().orElseThrow(() ->
                new IllegalStateException("Current operator is not available in SecurityContext"));
    }

    /**
     * 現在の操作者を取得する。取得できない場合は SYSTEM を返す。
     * （バッチ/初期化処理などに使用）
     */
    default Operator currentOrSystem() {
        return current().orElse(Operator.system());
    }

}
