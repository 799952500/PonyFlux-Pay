-- 按钮级权限迁移（与 Flyway V8 对齐，供 install_demo_db 等脚本引用）
USE payflow_admin;

ALTER TABLE sys_menus
    ADD COLUMN IF NOT EXISTS perm_code VARCHAR(128) NULL COMMENT '按钮权限码,仅 BUTTON 类型使用' AFTER status,
    ADD COLUMN IF NOT EXISTS api_pattern VARCHAR(256) NULL COMMENT '关联 API' AFTER perm_code;

-- 若列已存在则跳过；MySQL 8.0.12+ 不支持 IF NOT EXISTS on column，install 脚本以 schema 为准
