# Quickstart: 生产环境加固验证

**Feature**: 005-production-hardening
**Date**: 2026-05-15

## 前置条件

- Java 17 + Maven 3.8+
- MySQL 8.x（payflow_admin + payflow_cashier 数据库可用）
- Redis 可用
- Node.js 18+ （前端构建验证）

## 快速验证步骤

### 1. 构建所有模块

```bash
mvn -B -DskipTests compile
```

预期：全部 14 个模块 BUILD SUCCESS

### 2. 运行回归测试

```bash
mvn -B test
```

预期：现有 39 个测试全部通过，新增测试通过

### 3. 认证安全验证

```bash
# 验证未认证请求被拒绝
curl -s -o /dev/null -w "%{http_code}" http://localhost:3003/api/v1/merchants
# 预期：401

# 验证低权限角色无法创建管理员
curl -s -X POST http://localhost:3003/api/v1/admin/users \
  -H "Authorization: Bearer {ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"username":"hack","password":"test123"}'
# 预期：403
```

### 4. 敏感字段脱敏验证

```bash
# 验证商户响应不含 merchantKey
curl -s http://localhost:3003/api/v1/admin/merchants/M100001 \
  -H "Authorization: Bearer {ADMIN_TOKEN}" | jq '.data.merchantKey'
# 预期：null（字段被排除）
```

### 5. 商户管控验证

```bash
# 暂停商户后验证支付被拒绝
# 1. 通过管理后台将商户状态设为 SUSPENDED
# 2. 使用该商户密钥发起支付
curl -s -X POST http://localhost:3002/api/v1/merchant/orders \
  -H "Authorization: {HMAC_SIGNATURE}" \
  -H "Content-Type: application/json" \
  -d '{"subject":"test","amount":100}'
# 预期：code=5001, "商户已暂停服务"
```

### 6. Webhook 验证

```bash
# 启动一个本地 HTTP 监听器
# 完成一笔支付后验证收到 Webhook 通知
# 验证重试行为——关闭监听器看重试日志
```

### 7. 优雅关闭验证

```bash
# 发送 SIGTERM 并验证行为
kill -15 {pid}
# 观察日志：应打印"等待进行中的请求完成..."
# 30 秒后进程退出
```

## 关键环境变量（生产部署）

以下环境变量在生产环境 **必须** 设置，无默认值或默认值不安全：

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `JWT_SECRET` | JWT HS256 签名密钥（≥ 32 字符） | `openssl rand -base64 32` 生成 |
| `INTERNAL_TOKEN` | 服务间通信令牌 | `openssl rand -base64 32` 生成 |
| `PAYFLOW_CRYPTO_MASTER_KEY` | AES-256-GCM 主密钥 | `openssl rand -base64 32` 生成 |
| `DB_PASSWORD` | 数据库密码 | （各环境不同） |
| `CORS_ALLOWED_ORIGINS` | 允许的前端域名 | `https://admin.yourdomain.com,https://cashier.yourdomain.com` |

## 数据库迁移

```bash
# 执行本次变更的 DDL
mysql -u root -p payflow_cashier < sql/migrations/2026-05-15_production-hardening.sql
```

DDL 包含：`cashier_refunds` 添加 `version` 列、新增索引、添加 `NOT NULL` 约束。
