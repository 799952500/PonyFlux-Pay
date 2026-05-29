# Data Model: 产品质量优化与升级专项

**Created**: 2026-05-29  
**Feature**: [spec.md](spec.md)

> 本专项**不新增业务功能表**。本文描述为实现优化所需的**结构/语义变更**、**运行时实体**与**约束规则**。物理 DDL 通过 Flyway 增量迁移落地，并同步 `sql/schema/` 与 `install_demo_db.py`。

## 1) 既有实体变更

### 1.1 PayChannelAccount（cashier）— channelConfig 加密

**库**: `payflow_cashier`  
**表**: `cashier_pay_channel_accounts`（实体 `PayChannelAccount`）

| 变更 | 说明 |
|------|------|
| `channel_config` 存储格式 | 由明文 JSON → AES-256-GCM 密文（Base64），与 admin `PaymentAccount` 一致 |
| 读取 | MyBatis `EncryptedStringTypeHandler` 透明加解密 |
| 迁移 | 一次性脚本：扫描明文记录 → 加密写回；无法解密则标记账户 DISABLED 并告警 |

**验证**: SC-002 — 数据库直查无明文 `mch_key`/`apiV3Key` 等字段。

---

### 1.2 Payment（cashier）— 并发更新保护

**库**: `payflow_cashier`  
**表**: `cashier_payments`

| 变更 | 说明 |
|------|------|
| 方案 A（推荐） | 状态更新使用条件 SQL：`WHERE payment_id=? AND status='PROCESSING'` |
| 方案 B（可选） | 增加 `@Version` 乐观锁列 `version INT` |

**状态流转**（不变语义，强化并发）:

```text
PROCESSING → SUCCESS | FAILED | CLOSED
```

**规则**: `affectedRows==0` 时视为幂等命中，不触发 Webhook/MQ 副作用。

---

### 1.3 cashier_payments（可选中期）— bill_date 冗余列

**触发条件**: Decision 8 短期 SQL 改造后压测仍不达标。

| 字段 | 类型 | 说明 |
|------|------|------|
| `bill_date` | `DATE` | 账单日（对账/报表口径），由支付成功时间或任务写入 |
| 索引 | `(bill_date, status)` | 支撑范围查询替代 `DATE(updated_at)` |

**库**: `payflow_cashier` — 符合 `cashier_` 前缀。

---

## 2) 运行时 / 基础设施实体（非持久化表）

### 2.1 PaymentNotifyDedup（Redis）

| 属性 | 说明 |
|------|------|
| Key | `notify:dedup:{paymentId}:{eventType}` |
| TTL | 24h（与 JWT 有效期对齐可调） |
| 用途 | 防止 `PAYMENT_SUCCESS` Webhook/通知重复发送 |

---

### 2.2 JwtBlacklist（Redis，加固既有）

| 属性 | 说明 |
|------|------|
| Key | `jwt:blacklist:{jti}` |
| 写入失败 | 登出接口返回 503 或重试，**禁止** `catch ignored`（FR 关联 US1 运维项） |

---

### 2.3 UnifiedResponse `R<T>`（代码层，非表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 0=成功 |
| `message` | string | 用户可见文案 |
| `data` | T | 业务载荷 |

**位置**: `payflow-common` — 替代 admin `ApiResponse`、cashier/recon 各自 `R`。

---

### 2.4 JwtService（代码层，非表）

| Claim | admin | cashier |
|-------|-------|---------|
| `sub` | 用户 ID | 商户 ID |
| `jti` | UUID | UUID |
| `exp` | 24h | 可配置 |

**位置**: `payflow-common` — 单一实现，配置区分 issuer/secret。

---

## 3) 对账引擎数据访问变更（无新表）

### 3.1 批量写入契约

| 操作 | 原模式 | 新模式 |
|------|--------|--------|
| `recon_bill_record` insert | 逐条 | `saveBatch(500)` |
| `recon_diff` insert | 逐条 | `saveBatch(500)` |
| `recon_diff` heal update | 逐条 | 分批 `updateBatchById` |

**失败策略**: 单批失败 → 整批回滚 + 任务标记 FAIL + 日志含 `taskId/batchIndex`。

---

### 3.2 查询模式变更

| 场景 | 原模式 | 新模式 |
|------|--------|--------|
| 按账单日查支付 | `DATE(col)=?` | `col >= start AND col < end` |
| 长尾 open 列表 | N+1 `selectById` | `selectBatchIds` |

---

## 4) 分页请求语义（API 层）

### PageRequest（payflow-common）

| 字段 | 规则 |
|------|------|
| `page` | ≥1，默认 1 |
| `size` | `min(requested, 100)`，默认 20 |
| 响应 | 必须回传实际 `page`/`size`（spec Edge Case） |

**例外**: 对账工单等大列表可定义 `RECON_MAX_PAGE_SIZE=500` 常量，**禁止**无上限。

---

## 5) 前端状态模型（无后端表）

### PageLoadState（Vue composable 建议）

```text
idle → loading → success | error | empty
```

| 状态 | UI 要求 |
|------|---------|
| `loading` | `v-loading` 或 skeleton |
| `error` | 错误文案 + 重试按钮 |
| `empty` | `el-empty`（与 error 区分） |
| `success` | 正常内容 |

**适用页面**: `work-item-detail`、`report-detail`、`pc/index`、`h5/index` 等（FR-010~012）。

---

## 6) Flyway / Demo 一致性（元数据）

| 组件 | 当前问题 | 目标 |
|------|----------|------|
| `install_demo_db.py` | Flyway history 止于 V8/V5 | 对齐 admin **V11**、cashier **V5** |
| `docker-compose.yml` | 引用已删除 SQL | 使用 `install_demo_db` 或 schema+seed 挂载 |

**不涉及新表** — 仅修复迁移历史与初始化脚本一致性。

---

## 7) 实体关系简图

```text
[Order] 1──* [Payment] ──幂等/条件更新──> [NotifyDedup Redis]
                │
                └── channelConfig (encrypted) ──> [PayChannelAccount]

[ReconTask] 1──* [ReconBillRecord]  (batch insert)
           1──* [ReconDiff]         (batch insert)
```

---

## 8) 与 Key Entities（spec）映射

| spec Key Entity | 本文档章节 |
|-----------------|------------|
| Encrypted Channel Config | §1.1 |
| Unified Response R\<T\> | §2.3 |
| Unified JwtUtils | §2.4 |
| Payment Idempotency Key | §2.1 + §1.2 |
