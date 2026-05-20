-- =============================================================================
-- PonyFlux Pay — 演示库安装指引
--
-- 请按顺序在 MySQL 客户端中执行以下文件（每个文件整文件 Run）：
--
--   1. sql/schema/00_create_databases.sql
--   2. sql/schema/payflow_admin.sql
--   3. sql/schema/payflow_cashier.sql
--   4. sql/seed/payflow_cashier_seed.sql
--   5. sql/seed/payflow_admin_seed.sql
--
-- 命令行一键安装（推荐）：
--   python scripts/install_demo_db.py
--
-- 登录：admin / admin123
-- =============================================================================

SELECT '请使用 python scripts/install_demo_db.py 或按 sql/README.md 顺序执行各 SQL 文件' AS hint;
