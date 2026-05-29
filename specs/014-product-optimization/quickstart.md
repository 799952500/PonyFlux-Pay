# Quickstart: 产品质量优化与升级专项

**Created**: 2026-05-29  
**Feature**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)

本 Quickstart 按 **Wave 1→2→3** 提供验证步骤。每波实现完成后执行对应章节，并观察后台日志无阻断错误（宪法测试规范）。

## 0. 前置条件

```bash
# 初始化 demo 库（实现 Wave 2 后应含 Flyway V11 历史）
python scripts/install_demo_db.py

# 构建
mvn -B -DskipTests compile
```

环境变量参考（Wave 2 后使用 `.env.example`）：

```bash
export JWT_SECRET=dev-jwt-secret-change-me
export INTERNAL_TOKEN=dev-internal-token
export MASTER_KEY=dev-master-key-32bytes-minimum!!
export DB_PASSWORD=root
```

## Wave 1 — 支付安全 + 性能 + 前端体验（MVP）

### 启动服务

```bash
mvn -B -pl payflow-cashier-server spring-boot:run
mvn -B -pl payflow-admin-server spring-boot:run
mvn -B -pl payflow-recon-server spring-boot:run
```

```bash
cd payflow-admin-client && npm run dev
cd payflow-cashier-client && npm run dev
```

### 1. 微信回调验签（SC-001）

**目标**: 伪造签名被拒绝。

1. 创建一笔微信 Native 支付（PROCESSING）。
2. 向 `/notify/wechat` 发送**篡改签名**的回调 body。
3. **预期**: HTTP 失败；订单仍为 PROCESSING；日志含 `verify.fail`。

重复发送**合法**回调两次 → 订单仅 SUCCESS 一次；商户 Webhook 仅一次。

### 2. 收银台密钥加密（SC-002）

```sql
SELECT channel_config FROM cashier_pay_channel_accounts LIMIT 5;
```

**预期**: 值为密文 Base64，非明文 JSON。

### 3. 下单事务拆分（SC-003，压测）

使用 JMeter/ab 对 `POST /api/v1/payments` 200 并发，Mock 渠道延迟 2s。

**预期**: 本地 Payment 记录创建成功率 ≥99%；Hikari 无连接池耗尽错误。

### 4. 对账批量性能（SC-004）

准备 ~5 万条账单 fixture，执行一次对账任务。

**预期**: 总耗时较优化前下降 ≥60%（需实现前后各测一次记录 baseline）。

### 5. 分页上限（SC-005）

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:3003/api/v1/admin/merchants?page=1&pageSize=9999"
```

**预期**: 响应 `size` ≤ 100；`list.length` ≤ 100。

### 6. 前端静默失败修复（SC-006）

1. **停 admin-server** → 打开「对账 → 工单详情」→ 见错误卡片 + 重试。
2. **停 cashier-server** → 打开收银台付款页 → 见错误卡片 + 重试。
3. 二维码场景无 `paymentResult` 点「我已支付」→ loading 不复位卡死。
4. 空数据列表页 → 显示 `el-empty`。

```bash
cd payflow-admin-client && npx playwright test e2e/recon-work-items.spec.ts
cd payflow-cashier-client && npx playwright test
```

---

## Wave 2 — 一致性 + 运维 + i18n

### 7. 统一响应与 JWT（SC-007）

```bash
# 应无 ApiResponse 类（迁移完成后）
rg "class ApiResponse" payflow-admin-server --glob "*.java"

# 支付宝查单经 SPI（集成测试或手工）
```

**预期**: 仅 `com.payflow.common.web.R`；查单不依赖 `WxPayNativeHandler` 直接注入。

### 8. docker compose 一键启动（SC-008）

```bash
docker compose --env-file .env.example up -d
docker compose ps
```

**预期**: MySQL 初始化成功；三服务 `healthy`；无 `full-reseed` 相关错误。

### 9. recon 监控与日志（SC-009）

```bash
curl http://localhost:3004/actuator/prometheus | head
```

**预期**: 含 `recon.task.duration` 等指标；prod profile 日志无完整渠道响应 body。

### 10. i18n（SC-010）

1. admin-client 切换 en-US → 订单/商户/渠道页表头为英文。
2. cashier-client 收银台主文案走 `$t`。

---

## Wave 3 — 测试门禁 + 文档

### 11. 自动化测试（SC-011）

```bash
mvn -B clean test
# 查看 target/site/jacoco/index.html
```

**预期**: `PaymentNotifyServiceTest`、`ReconCompareServiceTest` 通过；JaCoCo 聚合 ≥40%。

### 12. CI E2E

推送分支后检查 GitHub Actions `e2e` job（或 nightly）。

**预期**: Playwright 对账 + 收银台冒烟通过。

### 13. 文档一致性

按 [docs/reconciliation.md](../../docs/reconciliation.md)（更新后）完成环境搭建：

- 无 `AdminReconClient` / `full-reseed` 死链
- 初始化仅用 `install_demo_db.py`

---

## 回归检查清单（每 Wave 结束）

- [ ] `mvn -B -DskipTests compile` 通过
- [ ] 三服务启动无 ERROR 日志
- [ ] 核心支付流程：下单 → 回调 → 订单 PAID
- [ ] 对账流程：任务 SUCCESS → 差异列表可查
- [ ] `docs/CONTRACT_MATRIX.md` 无破坏性变更（或已同步）
- [ ] Playwright 关键 spec 通过（Wave 1 起 admin，Wave 3 进 CI）
