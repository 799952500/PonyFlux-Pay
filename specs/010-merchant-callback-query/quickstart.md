# Quickstart: 商户回调记录查询

## 目标

验证：商户回调持久化、后台列表/详情查询、多次重试明细、商户隔离、敏感字段脱敏、与渠道回调语义分离。

## 前置条件

```bash
# 安装/更新 schema + seed（实现完成后包含新表与菜单）
python scripts/install_demo_db.py

# 启动服务（需 MQ 或接受 Worker 同步写库路径的集成测试替代）
mvn -B -pl payflow-cashier-server spring-boot:run
mvn -B -pl payflow-admin-server spring-boot:run
cd payflow-admin-client && npm run dev
```

演示账号密码: `python scripts/verify_admin_password.py`

## 验证路径

### 1. 支付成功回调落库（P1）

1. 通过收银台或 API 完成一笔演示订单支付，触发 `sendPaymentResultNotify`。
2. 确认 `cashier_merchant_notify` 存在 `notify_type=PAYMENT` 记录，`summary_status` 最终为 `SUCCESS` 或失败态。
3. 确认 `cashier_merchant_notify_attempt` 至少有 1 条，`request_params` 含 `orderId`、`status`、`sign`（库内完整）。

### 2. 列表与详情（P1/P2）

1. 登录管理端，打开 **交易与订单 → 商户回调记录** (`/admin/merchant-notifies`)。
2. 按平台订单号筛选，打开详情。
3. 确认：尝试序号递增、请求参数与响应可见、`sign` 为掩码显示。
4. 对比同屏 `orderStatus` 与 `notifyPayloadStatus`，理解状态不一致场景。

### 3. 失败重试明细（P1）

1. 将演示商户 `merchant_notify_url` 指向返回非 `success` 的测试地址（或 mock）。
2. 触发支付通知，等待 MQ 重试（60s / 300s / 900s 间隔，可缩短配置仅测试环境）。
3. 详情中应出现多条 `FAILED` 明细，汇总 `attempt_count` 与明细条数一致。

### 4. 未配置回调地址（Edge）

1. 创建无 `merchant_notify_url` 的订单并触发通知流程。
2. 查询汇总应为 `NOT_CONFIGURED`，明细为空，界面提示可读原因。

### 5. 商户数据隔离（P1）

1. 使用商户 A 管理员登录，查询商户 B 订单号。
2. 应拒绝或空结果，响应不泄露 B 的订单/回调是否存在。

### 6. 与渠道回调区分（FR-012）

1. 确认管理端「商户回调记录」菜单文案与渠道相关页面无混淆。
2. DB 中 `cashier_callback_logs` 仅渠道数据，新表仅商户通知数据。

### 7. Playwright（按需）

```bash
cd payflow-admin-client && npx playwright test
```

覆盖：菜单可见、列表筛选、详情打开、越权拒绝（若 seed 具备多商户账号）。

### 8. 后台日志闭环

观察 `payflow-cashier-server` 日志中 `[商户回调]` 与写库失败告警；排障过程无未处理异常堆栈。

## 验收对照

| 成功标准 | 验证方式 |
|----------|----------|
| SC-001 3 分钟内定位最近回调 | 计时走查列表→详情 |
| SC-002 关联正确率 100% | 抽样 order_id 与 notify 一致性 |
| SC-003 重试次数一致 | 对比 attempt_count 与明细条数 |
| SC-004 跨商户阻断 | 步骤 5 |
| SC-005 脱敏覆盖 | 步骤 2 检查 sign |
| SC-006 批量筛失败记录 | 筛选 `summaryStatus=FAILED` |
