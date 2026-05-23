-- 管理员 UI 偏好（主题、表格密度、侧栏折叠）按用户持久化
USE payflow_admin;
SET NAMES utf8mb4;

ALTER TABLE admin_users
  ADD COLUMN ui_theme VARCHAR(16) NOT NULL DEFAULT 'mint' COMMENT '主题：mint/ocean/violet/dark' AFTER data_merchant_ids,
  ADD COLUMN ui_table_density VARCHAR(16) NOT NULL DEFAULT 'standard' COMMENT '表格密度：standard/compact' AFTER ui_theme,
  ADD COLUMN ui_sidebar_collapsed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '侧栏是否折叠' AFTER ui_table_density;
