-- ================================================================
-- PonyFlux-Pay 用户管理初始数据
-- 数据库: payflow_admin
-- 说明: 包含 sys_users 表创建及初始用户数据
-- 密码说明: 所有密码均为 "admin123"，使用 BCrypt 加密
-- 生成方式: 运行 Java 程序通过 BCryptPasswordEncoder 生成
-- ================================================================

-- --------------------------------------------------------
-- 1. 创建 sys_users 表（如果不存在）
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
  nickname VARCHAR(128) COMMENT '显示名称',
  phone VARCHAR(32) COMMENT '手机号',
  email VARCHAR(128) COMMENT '邮箱',
  status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT 'ACTIVE=正常, DISABLED=禁用',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (username),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- --------------------------------------------------------
-- 2. 插入初始用户数据
-- 密码 "admin123" 的 BCrypt hash（Cost Factor=10）
-- 实际部署时建议通过程序重新生成以确保安全
-- --------------------------------------------------------

-- 管理员账户 (角色: SUPER_ADMIN)
INSERT IGNORE INTO sys_users (id, username, password, nickname, phone, email, status)
VALUES (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.sSLQo5FbZL1L6lyGFe', '系统管理员', '13800138000', 'admin@payflow.local', 'ACTIVE');

-- 运营人员账户 (角色: OPERATOR)
INSERT IGNORE INTO sys_users (id, username, password, nickname, phone, email, status)
VALUES (2, 'operator', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.sSLQo5FbZL1L6lyGFe', '运营人员', '13800138001', 'operator@payflow.local', 'ACTIVE');

-- 查看员账户 (角色: VIEWER, 禁用状态)
INSERT IGNORE INTO sys_users (id, username, password, nickname, phone, email, status)
VALUES (3, 'viewer', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.sSLQo5FbZL1L6lyGFe', '查看员', '13800138002', 'viewer@payflow.local', 'DISABLED');

-- --------------------------------------------------------
-- 附: BCrypt 密码生成参考（Java 代码示例）
-- --------------------------------------------------------
-- import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String hash = encoder.encode("admin123");
-- System.out.println(hash);
--
-- 附: Python pybcrypt 生成方式
-- pip install bcrypt
-- import bcrypt
-- hash = bcrypt.hashpw(b'admin123', bcrypt.gensalt(rounds=10))
-- print(hash.decode())


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
