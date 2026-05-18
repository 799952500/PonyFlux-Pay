# 快速验证：商户数据隔离与水平越权急修

**Feature**: 006-merchant-isolation  
**Date**: 2026-05-18

## 前置条件

- Java 17 + Maven 3.8+
- MySQL 8.x（`payflow_cashier` 已执行 Flyway 至 V4）
- Redis（JWT 黑名单、限流）
- 演示数据：`python scripts/run_mysql_sql.py sql/full-reseed-payflow-demo.sql`
- 至少两个测试商户 JWT（商户 A、商户 B）

```bash
# 启动收银台
mvn -B -pl payflow-cashier-server spring-boot:run

# 启动管理后台（审计列表验证）
mvn -B -pl payflow-admin-server spring-boot:run

# 启动管理前端
cd payflow-admin-client && npm run dev
```

## 1. 构建与测试

```bash
mvn -B -DskipTests compile
mvn -B -pl payflow-cashier-server,payflow-admin-server test -Dtest=MerchantIsolationSecurityTest,AdminSecurityAuditControllerTest
```

预期：新增安全测试全部通过；既有测试无回归失败。

## 2. merchantId 不一致 → 403 + 5101

```bash
# 1. 商户 A 登录获取 TOKEN_A
curl -s -X POST http://localhost:3002/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"M100001","password":"<demo_password>"}'

# 2. 携带 TOKEN_A，但请求体 merchantId 为商户 B
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:3002/api/v1/orders \
  -H "Authorization: Bearer {TOKEN_A}" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "M100002",
    "merchantOrderNo": "TEST-CROSS-001",
    "amount": 100,
    "subject": "越权测试"
  }'
```

预期：

- HTTP `403`
- body `"code":5101`
- `cashier_security_audit` 新增一条 `reason_code=5101`

## 3. 跨商户读订单 → 404 + 5102

```bash
# 假设 O-BELONGS-TO-B 属于商户 B
curl -s -w "\nHTTP:%{http_code}\n" \
  http://localhost:3002/api/v1/orders/O-BELONGS-TO-B \
  -H "Authorization: Bearer {TOKEN_A}"
```

预期：

- HTTP `404`
- body `"code":5102`，`message` 为「请求的资源不存在」
- 响应体**不含**订单金额、subject 等字段

对比：用商户 B 的 Token 请求同一 orderId 应返回 200。

## 4. 跨商户退款 → 404 + 5102

```bash
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://localhost:3002/api/v1/refunds \
  -H "X-Merchant-Id: M100001" \
  -H "X-Timestamp: $(date +%s)" \
  -H "X-Sign: {valid_hmac}" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "P-BELONGS-TO-B",
    "refundAmount": 50,
    "reason": "越权退款测试"
  }'
```

预期：HTTP 404 + code 5102；商户 B 的支付记录状态不变。

## 5. 兼容：不传 merchantId 或传一致值

```bash
# 不传 merchantId
curl -s -X POST http://localhost:3002/api/v1/orders \
  -H "Authorization: Bearer {TOKEN_A}" \
  -H "Content-Type: application/json" \
  -d '{"merchantOrderNo":"TEST-OK-001","amount":100,"subject":"正常下单"}'

# 传与 JWT 一致的 merchantId
curl -s -X POST http://localhost:3002/api/v1/orders \
  -H "Authorization: Bearer {TOKEN_A}" \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"M100001","merchantOrderNo":"TEST-OK-002","amount":100,"subject":"正常下单"}'
```

预期：均返回 `code:0`，订单 `merchant_id` 为 M100001。

## 6. 管理端审计列表

```bash
# RISK 或 SUPER_ADMIN Token
curl -s "http://localhost:3003/api/v1/admin/security/audit?page=1&pageSize=10&reasonCode=5101" \
  -H "Authorization: Bearer {ADMIN_TOKEN}"
```

预期：`code:0`，`data.records` 含步骤 2 产生的审计记录。

浏览器：登录管理后台 → **系统管理 → 安全审计** → 筛选商户号 / 时间范围。

## 7. 持久层拦截器抽查（开发环境）

在 `application-dev.yml` 开启：

```yaml
logging:
  level:
    com.payflow.cashier.mybatis: DEBUG
```

发起商户 A 的订单列表查询（HMAC `GET /api/v1/payment-links`），日志中应出现改写后 SQL 含 `merchant_id = 'M100001'`。

## 8. 回归检查清单

- [ ] 渠道回调 `/notify/**` 仍可正常更新支付状态
- [ ] 消费者访问 `/api/v1/cashier/{orderId}` 不受影响
- [ ] Payment Link 公开页 `/api/v1/public/payment-links/{linkId}` 可访问
- [ ] 现有 `mvn -B test` 全量通过
- [x] `docs/CONTRACT_MATRIX.md` 已更新 5101–5103 与审计 API

## 故障排查

| 现象 | 可能原因 |
|------|----------|
| 回调失败 | 回调路径未使用 `runInSystemMode` |
| 所有查询为空 | `MerchantContext` 未注入或过早 `clear()` |
| 审计表无数据 | 异步线程池未启用 `@EnableAsync` |
| admin 查询 403 | 角色非 RISK/SUPER_ADMIN |
