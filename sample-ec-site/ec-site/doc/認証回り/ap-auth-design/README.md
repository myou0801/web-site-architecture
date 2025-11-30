# AP基盤（認証）設計まとめ（分割版）

以下のファイルに分割しています。

1. 01_overview.md：分担、用語、仕様、設定値
2. 02_db_design_ddl.md：DDL（PostgreSQL/H2）、インデックス、Seed、Flyway
3. 03_domain_model.md：Value/Entity/Policy/例外
4. 04_mybatis_sql_templates.md：必要クエリ、Mapper XML雛形
5. 05_infrastructure_design.md：Record/Mapper I/F/RepositoryImpl方針、Clock、0件更新契約
6. 06_security_integration.md：SecurityConfig、UserDetails、Handler/Interceptor
7. 07_sharedservice_contracts.md：sharedService I/F、Validation契約、messageKey
8. 08_event_listeners.md：Success/Failureリスナー、Tx/ログ方針
