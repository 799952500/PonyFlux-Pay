-- 补全 admin_users 与 RBAC 角色关联（演示库种子在 audit_logs 处中断时缺失）
USE payflow_admin;
SET NAMES utf8mb4;

INSERT IGNORE INTO sys_user_roles (user_id, role_id, created_at) VALUES
(1, 1, NOW()),
(2, 2, NOW()),
(3, 4, NOW()),
(4, 2, NOW()),
(5, 2, NOW());
