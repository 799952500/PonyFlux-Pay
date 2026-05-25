-- 按钮级权限：admin_sys_menus 扩展字段 + BUTTON 节点种子

ALTER TABLE admin_sys_menus
    ADD COLUMN perm_code VARCHAR(128) NULL COMMENT '按钮权限码,仅 BUTTON 类型使用' AFTER status,
    ADD COLUMN api_pattern VARCHAR(256) NULL COMMENT '关联 API,格式 METHOD:/path/**' AFTER perm_code;

CREATE INDEX idx_admin_sys_menus_perm_code ON admin_sys_menus (perm_code);

-- 订单管理 (parent 11)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(201, 11, 'btn_order_export', '导出订单', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'order:export', 'GET:/api/v1/admin/orders/export', NOW(), NOW()),
(202, 11, 'btn_order_close', '关单', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'order:close', 'POST:/api/v1/admin/orders/*/close', NOW(), NOW()),
(203, 11, 'btn_refund_create', '申请退款', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'refund:create', 'POST:/api/v1/admin/orders/*/refund-requests', NOW(), NOW()),
(204, 11, 'btn_order_payment_query', '查单并同步', 'BUTTON', NULL, NULL, 4, 1, 'ACTIVE', 'order:payment:query', 'POST:/api/v1/admin/orders/*/payments/*/query-channel', NOW(), NOW());

-- 退款管理 (parent 12)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(205, 12, 'btn_refund_approve', '审批通过', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'refund:approve', 'POST:/api/v1/admin/refunds/*/approve', NOW(), NOW()),
(206, 12, 'btn_refund_reject', '审批拒绝', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'refund:reject', 'POST:/api/v1/admin/refunds/*/reject', NOW(), NOW());

-- 渠道管理 (parent 21)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(211, 21, 'btn_channel_create', '新增渠道', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'channel:create', 'POST:/api/v1/admin/channels', NOW(), NOW()),
(212, 21, 'btn_channel_edit', '编辑渠道', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'channel:edit', 'PUT:/api/v1/admin/channels/*', NOW(), NOW()),
(213, 21, 'btn_channel_delete', '删除渠道', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'channel:delete', 'DELETE:/api/v1/admin/channels/*', NOW(), NOW());

-- 支付方式 (parent 24)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(214, 24, 'btn_payment_method_create', '新增支付方式', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'payment_method:create', 'POST:/api/v1/admin/payment-methods', NOW(), NOW()),
(215, 24, 'btn_payment_method_edit', '编辑支付方式', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'payment_method:edit', 'PUT:/api/v1/admin/payment-methods/*', NOW(), NOW()),
(216, 24, 'btn_payment_method_delete', '删除支付方式', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'payment_method:delete', 'DELETE:/api/v1/admin/payment-methods/*', NOW(), NOW());

-- 支付账号 (parent 25)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(217, 25, 'btn_payment_account_create', '新增支付账号', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'payment_account:create', 'POST:/api/v1/admin/payment-accounts', NOW(), NOW()),
(218, 25, 'btn_payment_account_edit', '编辑支付账号', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'payment_account:edit', 'PUT:/api/v1/admin/payment-accounts/*', NOW(), NOW()),
(219, 25, 'btn_payment_account_delete', '删除支付账号', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'payment_account:delete', 'DELETE:/api/v1/admin/payment-accounts/*', NOW(), NOW());

-- 商户管理 (parent 31)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(221, 31, 'btn_merchant_edit', '编辑商户', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'merchant:edit', 'PUT:/api/v1/admin/merchants/*', NOW(), NOW()),
(222, 31, 'btn_merchant_delete', '删除商户', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'merchant:delete', 'DELETE:/api/v1/admin/merchants/*', NOW(), NOW());

-- 商户进件 (parent 102)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(223, 102, 'btn_onboarding_approve', '进件通过', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'onboarding:approve', 'POST:/api/v1/admin/onboarding/applications/*/approve', NOW(), NOW()),
(224, 102, 'btn_onboarding_reject', '进件拒绝', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'onboarding:reject', 'POST:/api/v1/admin/onboarding/applications/*/reject', NOW(), NOW());

-- 风控配置 (parent 32)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(225, 32, 'btn_risk_rule_write', '风控规则维护', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'risk:rule:write', 'POST:/api/v1/admin/risk/rules', NOW(), NOW());

-- 对账 (parent 60-63)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(231, 61, 'btn_recon_manual_run', '手动对账', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'recon:manual_run', 'POST:/api/v1/admin/reconcile/tasks/manual-run', NOW(), NOW()),
(232, 62, 'btn_recon_diff_handle', '差异处理', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'recon:diff:handle', 'POST:/api/v1/admin/reconcile/diffs/*/handle', NOW(), NOW());

-- 费率 (parent 65)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(233, 65, 'btn_fee_rate_write', '费率配置维护', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'fee_rate:write', 'POST:/api/v1/admin/fee-rate', NOW(), NOW());

-- 系统设置 (parent 41)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(241, 41, 'btn_system_config_write', '系统配置维护', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'system_config:write', 'POST:/api/v1/admin/system-configs', NOW(), NOW());

-- 角色管理 (parent 51)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(251, 51, 'btn_role_create', '新增角色', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'role:create', 'POST:/api/v1/admin/roles', NOW(), NOW()),
(252, 51, 'btn_role_edit', '编辑角色', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'role:edit', 'PUT:/api/v1/admin/roles/*', NOW(), NOW()),
(253, 51, 'btn_role_delete', '删除角色', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'role:delete', 'DELETE:/api/v1/admin/roles/*', NOW(), NOW()),
(254, 51, 'btn_role_assign_menu', '分配权限', 'BUTTON', NULL, NULL, 4, 1, 'ACTIVE', 'role:assign_menu', 'POST:/api/v1/admin/roles/*/menus', NOW(), NOW());

-- 菜单管理 (parent 52)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(255, 52, 'btn_menu_create', '新增菜单', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'menu:create', 'POST:/api/v1/admin/menus', NOW(), NOW()),
(256, 52, 'btn_menu_edit', '编辑菜单', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'menu:edit', 'PUT:/api/v1/admin/menus/*', NOW(), NOW()),
(257, 52, 'btn_menu_delete', '删除菜单', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'menu:delete', 'DELETE:/api/v1/admin/menus/*', NOW(), NOW());

-- 用户管理 (parent 53)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(258, 53, 'btn_user_create', '新增用户', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'user:create', 'POST:/api/v1/admin/users', NOW(), NOW()),
(259, 53, 'btn_user_edit', '编辑用户', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'user:edit', 'PUT:/api/v1/admin/users/*', NOW(), NOW()),
(260, 53, 'btn_user_reset_password', '重置密码', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'user:reset_password', 'PUT:/api/v1/admin/users/*/reset-password', NOW(), NOW());

-- 数据隔离治理 (parent 69)
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(261, 69, 'btn_data_isolation_remediate', '隔离治理修复', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'data_isolation:remediate', 'PUT:/api/v1/admin/data-isolation/checks/*/remediation', NOW(), NOW());

-- SUPER_ADMIN 全量授权按钮
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM admin_sys_menus WHERE id >= 201 AND menu_type = 'BUTTON';

-- ADMIN：除 role:delete、menu:delete、system_config:write 外全部按钮
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 2, id, NOW() FROM admin_sys_menus
WHERE menu_type = 'BUTTON' AND id >= 201
  AND perm_code NOT IN ('role:delete', 'menu:delete', 'system_config:write');

-- FINANCE：订单/退款相关
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 3, id, NOW() FROM admin_sys_menus
WHERE perm_code IN (
    'order:export', 'order:close', 'refund:create', 'order:payment:query',
    'refund:approve', 'refund:reject'
);

-- RISK：风控 + 对账差异处理
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 4, id, NOW() FROM admin_sys_menus
WHERE perm_code IN ('risk:rule:write', 'recon:diff:handle', 'recon:manual_run');

ALTER TABLE admin_sys_menus AUTO_INCREMENT = 300;
