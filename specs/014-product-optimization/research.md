# Phase 0 Research: 产品质量优化与升级专项

**Created**: 2026-05-29  
**Feature**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)

本阶段将技术上下文中的不确定点收敛为可落地决策，支撑 Phase 1 设计与 Phase 2 任务拆解。本专项**不新增对外业务 API**，以加固、性能、一致性、可观测性与工程化为主。

---

## Decision 1: 分三波交付（Wave），MVP = Wave 1（P1）

- **Decision**:
  - **Wave 1（MVP）**：US1 支付安全/回调 + US2 性能 + US3 前端静默失败修复（FR-001~012）
  - **Wave 2**：US4 一致性收敛 + US5 运维就绪 + US6 i18n（FR-013~022）
  - **Wave 3**：US7 测试门禁 + US8 文档（FR-023~027）
- **Rationale**: spec 已明确 P1/P2/P3；Wave 1 直接消除资金风险与可用性瓶颈；Wave 2 降低维护成本；Wave 3 建立长期回归网。
- **Alternatives considered**:
  - 全量一次交付：风险高、回归面过大；
  - 仅做文档/测试：无法解决 P0 级支付安全问题。

---

## Decision 2: 微信 v3 回调验签 = 平台证书 RSA + 时间戳防重放

- **Decision**: 在 `WxPayNotifyHelper` / `WxPayOpenService` 回调入口补齐：① 使用 `Wechatpay-Signature` 等头 + 平台证书公钥验签；② `timestamp` 落在 ±5 分钟窗口；③ 验签失败返回失败并 `log.warn` + 指标计数，**不进入解密与业务处理**。平台证书/序列号从现有 `channelConfig` JSON 读取；缺证书账户回调拒绝并告警（spec Edge Case）。
- **Rationale**: 与支付宝/银联已有 RSA 验签对齐；符合 FR-001/SC-001；不引入 Vault。
- **Alternatives considered**:
  - 仅 AES 解密：现状，不满足安全要求；
  - 独立证书服务：超出范围。

---

## Decision 3: 支付回调幂等 = 条件更新 + Redis 通知去重

- **Decision**:
  - DB 层：`UPDATE cashier_payments SET status='SUCCESS' WHERE payment_id=? AND status='PROCESSING'`（或 `@Version` 乐观锁），`affectedRows==0` 视为已处理；
  - 通知层：商户 Webhook / 站内通知使用 Redis key `notify:dedup:{paymentId}:PAYMENT_SUCCESS`，TTL 24h；
  - `paidImmediately` 与异步回调叠加：成功路径先写 dedup key 再发通知。
- **Rationale**: 满足 FR-002/FR-003；比新建幂等表改动小；与现有 Redis 基础设施一致。
- **Alternatives considered**:
  - 仅 DB 乐观锁：无法阻止重复 Webhook 若两次都更新成功（竞态窗口）；
  - 独立 `payment_event` 表：更重，留作后续增强。

---

## Decision 4: 收银台 `channelConfig` 加密 = 复用 Admin TypeHandler 模式

- **Decision**: 为 `PayChannelAccount.channelConfig` 增加与 admin `PaymentAccount` 相同的 `EncryptedStringTypeHandler` + `payflow.crypto.master-key`；存量明文数据通过一次性迁移脚本或启动时 lazy 加密（读明文→写密文）。加解密逻辑复用 `payflow-common` 的 `AesEncryptor`。
- **Rationale**: FR-004/SC-002；与宪法 V 一致；避免两套加密实现。
- **Alternatives considered**:
  - 仅 admin 加密、cashier 运行时从 admin 拉取不落库：增加运行时依赖与延迟；
  - 字段级应用层手动加解密：易漏、难维护。

---

## Decision 5: 下单事务拆分 = 短事务落库 + 事务外调渠道

- **Decision**:
  1. **Tx1（短）**：创建/更新 Order + Payment（PROCESSING）；
  2. **无事务**：`PayChannelPaymentOpenService.pay()` 调渠道；
  3. **Tx2（短）**：根据结果更新 Payment/Order；失败标记明确终态或待查单。
  - 不在 Tx1 内调用 HTTP；渠道失败不回滚已创建的本地记录（由超时/查单兜底，spec Edge Case）。
- **Rationale**: FR-005/SC-003；标准支付网关模式。
- **Alternatives considered**:
  - 保持单事务：现状，连接池风险仍在；
  - 全异步 MQ 下单：改动过大，非本期范围。

---

## Decision 6: 对账批量写入 = MyBatis-Plus `saveBatch` / 分批 UPDATE

- **Decision**: `ReconExecuteService.persistBillRecords`、`ReconCompareService` 差异写入、`ReconDiffHealService.annotateSuggestions` 统一改为 batch size **500**（可配置 `payflow.recon.batch-size`）；单批失败整批回滚并记录 `task_id + batchIndex`（spec Edge Case）。
- **Rationale**: FR-006/SC-004；MyBatis-Plus 已依赖，无新中间件。
- **Alternatives considered**:
  - JDBC batch 手写：重复造轮子；
  - 异步队列写 diff：引入 MQ 复杂度。

---

## Decision 7: 分页上限 = `payflow-common` 统一 `PageRequest` 工具

- **Decision**: 新增 `PageRequest.of(page, size)` 强制 `size = min(requested, 100)`；admin/cashier Controller 与 Service 统一使用；假分页接口（如 `PaymentAccountController.listAll`）改为 DB 分页查询。
- **Rationale**: FR-007/SC-005；一处修复、全局受益。
- **Alternatives considered**:
  - 各 Controller 自行 `Math.min`：易遗漏；
  - 上限 500（宪法对账页）：本专项对**通用列表**采用 100，对账专用接口可保留 500 但须显式常量区分。

---

## Decision 8: 日期聚合查询 = 半开区间范围查询（优先）+ 可选 `bill_date` 冗余列

- **Decision**:
  - **短期**：将 `DATE(COALESCE(updated_at, created_at)) = ?` 改为 `>= dayStart AND < dayEnd`；
  - **中期（若压测仍慢）**：在 `cashier_payments` 增加 `bill_date DATE` + 索引 `(bill_date, status)`，由写入/对账任务维护。
- **Rationale**: FR-009；先低成本改造，再按需加列。
- **Alternatives considered**:
  - 仅加冗余列：DDL 成本更高；
  - 仅改 SQL 不改索引：可能仍不足，保留中期选项。

---

## Decision 9: 一致性收敛 = `payflow-common` 承载 `R<T>` + `JwtService`

- **Decision**:
  - 将 cashier/recon 的 `R<T>` 迁入 `payflow-common`（或统一包名 `com.payflow.common.web`）；
  - admin `GlobalExceptionHandler.ApiResponse` 逐步废弃，改为 `R` + 同一 Advice 模式；
  - `JwtUtils` 合并为 `JwtService`（Spring Bean），admin/cashier 通过配置区分 issuer/secret/claims；**迁移期允许双轨 1 个迭代**，在 Complexity Tracking 记录。
- **Rationale**: FR-013/FR-014/SC-007；直接落实宪法 IV。
- **Alternatives considered**:
  - 仅文档约定不统一代码：无法通过 SC-007；
  - Map 手工返回：现状，维护成本高。

---

## Decision 10: 查单/关单 = 扩展 `PayChannelPaymentOpenService.queryOrder`

- **Decision**: 在 payment-core 定义 `queryOrder(QueryContext)`；各渠道 OpenService 实现；`PaymentQueryServiceImpl`、`OrderTimeoutTask`、`OrderMqConsumer` 全部改经 `PayChannelPaymentOpenServiceLocator`；删除对 `WxPayNativeHandler` 的直接注入。
- **Rationale**: FR-015；落实宪法 II；修复超时关单恒 false 问题。
- **Alternatives considered**:
  - 各 Service 继续 if-else 渠道：违反抽象原则。

---

## Decision 11: 运维修复 = compose 调用 `install_demo_db.py` + Flyway V11 对齐

- **Decision**:
  - `docker-compose.yml` MySQL init 改为挂载 `scripts/docker-init/`（包装调用 `python scripts/install_demo_db.py` 或拆分为 `schema`+`seed` 挂载）；
  - 更新 `install_demo_db.py` 的 `FLYWAY_*_MIGRATIONS` 至 **V11**；
  - 三 Java 服务增加 `healthcheck`；新增根目录 `.env.example`。
- **Rationale**: FR-018~020/SC-008；修复当前 compose 必败问题。
- **Alternatives considered**:
  - 恢复 `full-reseed` 单文件：已废弃，与 Flyway 双轨冲突。

---

## Decision 12: 可观测性 = recon 对齐 Actuator + 自定义 Micrometer 指标

- **Decision**: recon-server `application.yml` 暴露 `health,info,metrics,prometheus`；注册 `recon.task.duration`、`recon.task.failures`、`recon.diff.count`；cashier/recon 默认日志级别 prod=INFO；渠道错误 body 日志截断至 200 字符且仅 DEBUG。
- **Rationale**: FR-016/FR-017/SC-009。
- **Alternatives considered**:
  - 仅 health：不足以排障对账积压。

---

## Decision 13: i18n = 按模块分批抽取 + 中文回退

- **Decision**: admin 优先 `orders`、`merchants`、`channels`、`users`、`reconcile/*`；cashier 注册 i18n + `pc/index`、`h5/index`、`receipt`；`vue-i18n` fallbackLocale=`zh-CN`；缺失 key 显示中文默认值。
- **Rationale**: FR-021/FR-022/SC-010；控制 Wave 2 工作量。
- **Alternatives considered**:
  - 全量一次性抽取：PR 过大、易冲突。

---

## Decision 14: 测试门禁 = 核心单测 + CI E2E job（可选 nightly）

- **Decision**:
  - 根 `pom.xml` 增加 JaCoCo，**初期门禁 40%**（模块聚合），每季度 +10%；
  - 新增 `PaymentNotifyServiceTest`（Mock）、`ReconCompareServiceTest`（fixture CSV）；
  - CI 新增 `e2e` job：MySQL service + seed + 三后端 + `npx playwright test`（允许先 nightly 再 required）；
  - `DashboardMetricsMapperTest` 改为 `@EnabledIfDatabaseAvailable` 或 Testcontainers。
- **Rationale**: FR-023~025/SC-011；与 spec Assumptions 一致（不要求一次 80%）。
- **Alternatives considered**:
  - 一次 80%：不现实，会导致假绿或大面积 exclude。

---

## Decision 15: N+1 修复清单（Wave 1 内并行）

- **Decision**: 按审计报告逐项修复，优先高频路径：
  - `MerchantNotifyQueryServiceImpl`：批量查 Order + 通知触发移出分页循环；
  - `PaymentAccountServiceImpl.getById`：单条 JOIN；
  - `PayChannelServiceImpl.toDTO`：批量 channel；
  - `ReconLongTailService`：`selectBatchIds`；
  - `AliPayNotifyHelper.findAlipayPublicKey`：按 appId 索引（DB 列或 Redis 缓存）。
- **Rationale**: FR-008；与 Wave 1 性能目标一致。
- **Alternatives considered**:
  - 仅修对账：遗漏支付回调热点路径。

---

## Open Items Resolved

| 原 NEEDS CLARIFICATION | 决议 |
|------------------------|------|
| Wave 切分 | Decision 1 |
| 微信验签实现方式 | Decision 2 |
| 幂等策略 | Decision 3 |
| 下单事务边界 | Decision 5 |
| 响应/JWT 统一策略 | Decision 9（含短期双轨） |
| compose/Flyway 修复 | Decision 11 |
| JaCoCo 目标 | Decision 14（40% 起步） |

**Phase 0 结论**：无未决 NEEDS CLARIFICATION，可进入 Phase 1。
