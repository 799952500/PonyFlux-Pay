#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Append sys_roles init data to users-init-data.sql"""
import sys
sys.stdout.reconfigure(encoding='utf-8')

# Read existing content
with open(r'D:\个人\pay\PonyFlux-Pay\sql\admin\users-init-data.sql', 'r', encoding='utf-8') as f:
    content = f.read()

# Add sys_roles section at the end
roles_section = '''

-- --------------------------------------------------------
-- 3. 创建 sys_roles 表（如果不存在）
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(128) NOT NULL,
  description VARCHAR(512),
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_role_code (role_code),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- --------------------------------------------------------
-- 4. 插入初始角色数据
-- --------------------------------------------------------

-- 超级管理员
INSERT IGNORE INTO sys_roles (id, role_code, role_name, description, status)
VALUES (1, 'SUPER_ADMIN', '超级管理员', '拥有系统所有权限', 'ACTIVE');

-- 管理员
INSERT IGNORE INTO sys_roles (id, role_code, role_name, description, status)
VALUES (2, 'ADMIN', '管理员', '拥有大部分管理权限', 'ACTIVE');

-- 财务人员
INSERT IGNORE INTO sys_roles (id, role_code, role_name, description, status)
VALUES (3, 'FINANCE', '财务人员', '负责财务相关操作', 'ACTIVE');

-- 风控人员
INSERT IGNORE INTO sys_roles (id, role_code, role_name, description, status)
VALUES (4, 'RISK', '风控人员', '负责风控管理操作', 'ACTIVE');
'''

new_content = content + roles_section

# Write with UTF-8 BOM (Windows SQL file standard)
with open(r'D:\个人\pay\PonyFlux-Pay\sql\admin\users-init-data.sql', 'w', encoding='utf-8-sig') as f:
    f.write(new_content)

print('Updated users-init-data.sql successfully')
print('New length:', len(new_content))