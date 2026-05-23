-- 商户入驻申请闭环：扩展 merchant_application 字段
USE payflow_admin;
SET NAMES utf8mb4;

ALTER TABLE merchant_application
  ADD COLUMN contact_email VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱' AFTER contact_phone,
  ADD COLUMN application_source VARCHAR(32) NOT NULL DEFAULT 'CASHIER_PUBLIC' COMMENT '申请来源' AFTER status,
  ADD COLUMN allocated_merchant_id VARCHAR(64) DEFAULT NULL COMMENT '审批分配的商户号' AFTER application_source,
  ADD COLUMN secret_cipher VARCHAR(512) DEFAULT NULL COMMENT 'AES加密后的密钥载荷' AFTER allocated_merchant_id,
  ADD COLUMN secret_viewed_at DATETIME DEFAULT NULL COMMENT '首次查看密钥时间' AFTER secret_cipher,
  ADD COLUMN result_query_count INT NOT NULL DEFAULT 0 COMMENT '自助查询次数' AFTER secret_viewed_at,
  ADD COLUMN approver_id BIGINT DEFAULT NULL COMMENT '审批人ID' AFTER result_query_count,
  ADD COLUMN approved_at DATETIME DEFAULT NULL COMMENT '审批通过时间' AFTER approver_id,
  ADD COLUMN rejected_at DATETIME DEFAULT NULL COMMENT '拒绝时间' AFTER approved_at;

ALTER TABLE merchant_application MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED';

-- MERCHANT_ADMIN 角色与菜单（订单/退款/回调 + 工作台概览）
INSERT INTO sys_roles (role_code, role_name, description, status, created_at, updated_at)
SELECT 'MERCHANT_ADMIN', '商户管理员', '商户入驻后自助查询与订单只读', 'ACTIVE', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_roles WHERE role_code = 'MERCHANT_ADMIN');

SET @merchant_admin_role_id = (SELECT id FROM sys_roles WHERE role_code = 'MERCHANT_ADMIN' LIMIT 1);

DELETE FROM sys_role_menus WHERE role_id = @merchant_admin_role_id;
INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT @merchant_admin_role_id, id, NOW() FROM sys_menus
WHERE id IN (1, 2, 10, 11, 12, 13);

UPDATE merchant_application
SET application_source = 'CASHIER_PUBLIC'
WHERE application_source IS NULL OR application_source = '';

UPDATE merchant_application
SET allocated_merchant_id = 'M100002', approved_at = updated_at
WHERE application_no = 'KYB-20260510-002' AND status = 'APPROVED';

UPDATE merchant_application
SET rejected_at = updated_at
WHERE application_no = 'KYB-20260505-003' AND status = 'REJECTED';
