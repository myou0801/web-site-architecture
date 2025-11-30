# Infrastructure（Record/Mapper I/F/RepositoryImpl/Clock/0件更新契約）

方針：
- TypeHandlerを使わず、RepositoryImplで **Record ↔ Domain 変換**してMapperを呼ぶ
- Record命名：`*Record`
- Repository実装命名：`*RepositoryImpl`

---

## 1. insert時のID採番（record immutabilityへの対処）

recordはimmutableなので `useGeneratedKeys` の keyProperty書き戻しと相性が悪い。

採用案（確定）：
- `AUTH_ACCOUNT` INSERT後、Tx内で `user_id`（UNIQUE）で再SELECTし `auth_account_id` を取得する

---

## 2. Clock（時刻供給）

AP基盤で `Clock` をBean提供（確定）：
```java
@Bean
public Clock clock() {
    return Clock.systemDefaultZone();
}
```

---

## 3. 0件更新（updateが0件）時の契約（確定：推奨）

方針（採用）：
- sharedService（Admin/PasswordChange）は先に `findById` で存在/削除を判定
- その上で update を実行し、それでも0件なら **IllegalStateException**（競合保険）扱い
- 画面入力に紐づくものは `ValidationException`、整合性異常は 500 でよい

---

## 4. RepositoryImpl（設計意図）
- RepositoryImplは「変換と呼び出し」に責務を限定
- 例外変換/メッセージは sharedService（application）で行う
