-- 014-product-optimization: 收银台渠道配置加密迁移说明
-- 新写入由 EncryptedStringTypeHandler 自动加密；存量明文请在应用启动后通过运维脚本或
-- payflow.crypto.master-key 配置下由 EncryptedMigrationRunner 批量加密（实现阶段可选）。

-- 本文件为占位迁移记录，不执行破坏性 UPDATE，避免无 master-key 环境下损坏 demo 数据。
