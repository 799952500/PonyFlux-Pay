# Tasks: 产品质量优化与升级专项

**Input**: Design documents from `/specs/014-product-optimization/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Organization**: 任务按用户故事（US1→US8）分组，对应 spec 优先级 P1→P3 与 plan 的 Wave 1→3。每个故事可独立验收。测试任务集中在 **US7**（spec 明确要求 FR-023~025）。

**Format**: `[ID] [P?] [Story] Description` — 含精确文件路径

---

## Phase 1: Setup（文档与基线对齐）

**Purpose**: 锁定契约与验收口径，建立优化前后对比基线

- [x] T001 锁定行为契约 `specs/014-product-optimization/contracts/payment-security-contract.md`
- [x] T002 [P] 锁定行为契约 `specs/014-product-optimization/contracts/shared-platform-contract.md`
- [x] T003 [P] 锁定行为契约 `specs/014-product-optimization/contracts/frontend-ux-contract.md`
- [x] T004 [P] 锁定行为契约 `specs/014-product-optimization/contracts/observability-ops-contract.md`
- [x] T005 对齐分 Wave 验收步骤 `specs/014-product-optimization/quickstart.md`（记录对账 5 万条 / 200 并发压测 baseline 占位）

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 各 User Story 共享的基础设施与配置  
**⚠️ CRITICAL**: 完成前不开始 US1 实现（Setup 文档任务除外）

- [x] T006 [P] 新增统一分页工具 `payflow-common/src/main/java/com/payflow/common/web/PageRequest.java`（`size` 上限 100）
- [x] T007 [P] 新增分页结果包装 `payflow-common/src/main/java/com/payflow/common/web/PageResult.java`
- [x] T008 [P] 新增通知去重 Redis 服务 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/NotifyDedupService.java`（key: `notify:dedup:{paymentId}:{eventType}`）
- [x] T009 [P] 配置对账批大小默认值 `payflow-recon-server/src/main/resources/application.yml`（`payflow.recon.batch-size: 500`）
- [x] T010 确保 `payflow-common` 的 `AesEncryptor` 可被 cashier 模块引用（检查 `payflow-cashier-server/pom.xml` 依赖）

**Checkpoint**: `mvn -B -pl payflow-common,payflow-cashier-server,payflow-recon-server -DskipTests compile` 通过

---

## Phase 3: User Story 1 - 支付资金安全与回调正确性加固 (Priority: P1) 🎯 MVP 核心

**Goal**: 微信验签+防重放、回调幂等、channelConfig 加密、并发状态更新安全（FR-001~004）  
**Independent Test**: 见 `specs/014-product-optimization/spec.md` US1；`quickstart.md` Wave 1 §1~2

**Contract**: `contracts/payment-security-contract.md`

### Implementation for User Story 1

- [x] T011 [P] [US1] 实现微信 v3 RSA 平台证书验签 + 时间戳 ±300s 校验 `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/wxpay/WxPayNotifyHelper.java`
- [x] T012 [US1] 回调入口强制「先验签后解密」并注册失败指标 `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/impl/WxPayOpenService.java`
- [x] T013 [US1] 支付成功条件更新（`WHERE status='PROCESSING'`）`payflow-cashier-server/src/main/java/com/payflow/cashier/service/PayNotifyService.java`
- [x] T014 [US1] 接入 `NotifyDedupService`，重复回调与 `paidImmediately` 路径仅触发一次 Webhook `payflow-cashier-server/src/main/java/com/payflow/cashier/service/PayNotifyService.java`
- [x] T015 [US1] 同步下单即时成功路径写入 dedup key `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PaymentServiceImpl.java`
- [x] T016 [P] [US1] 复用 admin 加密 TypeHandler 至 cashier `payflow-cashier-server/src/main/java/com/payflow/cashier/config/EncryptedStringTypeHandler.java`（参考 `payflow-admin-server/src/main/java/com/payflow/admin/config/EncryptedStringTypeHandler.java`）
- [x] T017 [US1] `PayChannelAccount.channelConfig` 字段启用 TypeHandler `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/PayChannelAccount.java`
- [x] T018 [US1] 存量明文 `channel_config` 加密迁移脚本 `sql/migrations/2026-05-29_cashier_channel_config_encrypt.sql`
- [x] T019 [US1] 缺平台证书账户回调拒绝并告警（日志 + metric）`payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/wxpay/WxPayNotifyHelper.java`
- [x] T020 [US1] 登出黑名单写入禁止吞异常 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AuthController.java`

**Checkpoint**: 伪造微信回调被拒绝；合法回调重复仅确认一次；DB 中 `channel_config` 为密文

---

## Phase 4: User Story 2 - 高并发下单与对账吞吐性能优化 (Priority: P1)

**Goal**: 下单事务拆分、对账批量写、分页上限、N+1 修复、日期查询可走索引（FR-005~009）  
**Independent Test**: 见 `spec.md` US2；`quickstart.md` Wave 1 §3~5

### Implementation for User Story 2

#### 2.1 下单与缓存

- [x] T021 [US2] 拆分 `createPayment`：短事务落库 → 事务外调渠道 → 短事务更新结果 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PaymentServiceImpl.java`
- [x] T022 [US2] 修复 Cache-Aside 顺序（先 evict 再 update）`payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/OrderServiceImpl.java`

#### 2.2 对账批量写入

- [x] T023 [US2] 账单解析批量 insert（`saveBatch`）`payflow-recon-server/src/main/java/com/payflow/recon/service/ReconExecuteService.java`
- [x] T024 [US2] 差异比对批量 insert `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconCompareService.java`
- [x] T025 [US2] 差异标注批量 update `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconDiffHealService.java`
- [x] T026 [US2] 单批失败整批回滚并记录 `taskId/batchIndex` `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconExecuteService.java`

#### 2.3 分页与假分页消除

- [x] T027 [P] [US2] 支付账号列表改 DB 分页 + `PageRequest` `payflow-admin-server/src/main/java/com/payflow/admin/controller/PaymentAccountController.java`
- [x] T028 [P] [US2] 商户列表强制 `pageSize` 上限 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMerchantController.java`
- [x] T029 [P] [US2] 流失预警列表强制 `pageSize` 上限 `payflow-admin-server/src/main/java/com/payflow/admin/service/ChurnAlertService.java`

#### 2.4 N+1 修复

- [x] T030 [US2] 回调列表批量查 Order，通知触发移出分页循环 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/MerchantNotifyQueryServiceImpl.java`
- [x] T031 [P] [US2] `getById` 改单条 JOIN 查询 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/PaymentAccountServiceImpl.java`
- [x] T032 [P] [US2] 渠道账户 DTO 批量查 channel `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PayChannelServiceImpl.java`
- [x] T033 [P] [US2] 长尾统计 `selectBatchIds` 替代逐条 `selectById` `payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconLongTailService.java`
- [x] T034 [US2] 支付宝回调按 appId 索引/缓存公钥，消除全表扫描 `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/alipay/AliPayNotifyHelper.java`
- [x] T035 [P] [US2] 路由同步批量 `selectBatchIds` `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/MerchantCashierRouteSyncServiceImpl.java`
- [x] T036 [US2] 最低成本路由一次查全量账户后内存分组 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PayChannelServiceImpl.java`

#### 2.5 日期查询索引友好

- [x] T037 [P] [US2] 对账支付查询改半开区间 `payflow-recon-server/src/main/java/com/payflow/recon/mapper/CashierReconPaymentMapper.java`
- [x] T038 [P] [US2] 报表 Mapper 去除 `DATE()` 包裹 `payflow-admin-server/src/main/java/com/payflow/admin/mapper/ReconCashierReportMapper.java`
- [x] T039 [P] [US2] Dashboard 聚合改范围查询 `payflow-admin-server/src/main/java/com/payflow/admin/service/DashboardAggregationService.java`
- [x] T040 [US2]（可选，压测不达标时）新增 `bill_date` 列与索引 Flyway `payflow-cashier-server/src/main/resources/db/migration/cashier/V6__payments_bill_date.sql`

**Checkpoint**: 分页 `pageSize=9999` 时实际返回 ≤100；对账大批量耗时显著下降

---

## Phase 5: User Story 3 - 关键操作路径不再静默失败 (Priority: P1)

**Goal**: 关键页 loading/error/empty/retry；收银台 loading 不复位卡死（FR-010~012）  
**Independent Test**: 见 `spec.md` US3；`quickstart.md` Wave 1 §6

**Contract**: `contracts/frontend-ux-contract.md`

### Implementation for User Story 3

#### 3.1 P0 页面（静默失败修复）

- [x] T041 [US3] 工单详情：loading/error/retry + 操作按钮 `:loading` + catch `payflow-admin-client/src/pages/admin/reconcile/work-item-detail.vue`
- [x] T042 [P] [US3] 报告详情：catch + `el-empty` `payflow-admin-client/src/pages/admin/reconcile/report-detail.vue`
- [x] T043 [P] [US3] 通知 Popover：失败 `ElMessage.warning` `payflow-admin-client/src/components/NotificationPopover.vue`
- [x] T044 [P] [US3] SLA 保存：`submitting` + catch `payflow-admin-client/src/pages/admin/reconcile/sla-rules.vue`
- [x] T045 [P] [US3] 漏斗页商户下拉失败提示 `payflow-admin-client/src/pages/admin/insights-funnel.vue`
- [x] T046 [P] [US3] 渠道路由辅助数据失败提示 `payflow-admin-client/src/pages/admin/channel-routes.vue`

#### 3.2 收银台

- [x] T047 [US3] 「我已支付」异常分支复位 `confirming` `payflow-cashier-client/src/composables/useCashierCheckout.ts`
- [x] T048 [P] [US3] PC 收银台错误卡片 + 重试 `payflow-cashier-client/src/pages/cashier/pc/index.vue`
- [x] T049 [P] [US3] H5 收银台错误卡片 + 重试 `payflow-cashier-client/src/pages/cashier/h5/index.vue`
- [x] T050 [US3] 支付轮询连续失败 ≥3 次停止并提示 `payflow-cashier-client/src/composables/useCashierCheckout.ts`

#### 3.3 拦截器与依赖

- [x] T051 [P] [US3] 401/403 友好提示 `payflow-admin-client/src/api/request.ts`
- [x] T052 [P] [US3] 显式添加 `dayjs` 依赖 `payflow-admin-client/package.json`

#### 3.4 空状态（批量）

- [x] T053 [P] [US3] 用户/角色/订单等列表补 `el-empty` `payflow-admin-client/src/pages/admin/users.vue`
- [x] T054 [P] [US3] 角色列表补 `el-empty` `payflow-admin-client/src/pages/admin/roles.vue`
- [x] T055 [P] [US3] 订单列表补 `el-empty` `payflow-admin-client/src/pages/admin/orders/index.vue`
- [x] T056 [P] [US3] 系统设置页 `v-loading` + 空态 `payflow-admin-client/src/pages/admin/settings.vue`

**Checkpoint**: 停后端后工单详情/收银台显示错误+重试，无白屏/永久 loading

---

## Phase 6: User Story 4 - 系统一致性与可维护性收敛 (Priority: P2)

**Goal**: 统一 `R<T>`/`JwtService`、查单 SPI、超时真实查单（FR-013~015）  
**Independent Test**: 见 `spec.md` US4；`quickstart.md` Wave 2 §7

**Contract**: `contracts/shared-platform-contract.md`、`contracts/payment-security-contract.md`

### Implementation for User Story 4

- [x] T057 [P] [US4] 迁入统一响应体 `payflow-common/src/main/java/com/payflow/common/web/R.java`
- [x] T058 [P] [US4] 实现统一 `JwtService`（含 `jti`）`payflow-common/src/main/java/com/payflow/common/security/JwtService.java`
- [x] T059 [US4] admin 全局异常处理改用 `R`（保留迁移期兼容）`payflow-admin-server/src/main/java/com/payflow/admin/exception/GlobalExceptionHandler.java`
- [x] T060 [P] [US4] cashier/recon `R` 改引用 `payflow-common` `payflow-cashier-server/src/main/java/com/payflow/cashier/common/R.java`
- [x] T061 [P] [US4] 删除或废弃 admin 本地 `JwtUtils`，改用 `JwtService` `payflow-admin-server/src/main/java/com/payflow/admin/util/JwtUtils.java`
- [x] T062 [P] [US4] 删除或废弃 cashier 本地 `JwtUtils` `payflow-cashier-server/src/main/java/com/payflow/cashier/util/JwtUtils.java`
- [x] T063 [US4] `PayChannelPaymentOpenService` 新增 `queryOrder` 方法 `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/payment/PayChannelPaymentOpenService.java`
- [x] T064 [P] [US4] 微信渠道实现 `queryOrder` `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/payment/impl/WxPayPaymentOpenService.java`
- [x] T065 [P] [US4] 支付宝渠道实现 `queryOrder` `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/impl/AliPayPaymentOpenService.java`
- [x] T066 [P] [US4] 银联渠道实现 `queryOrder` `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/impl/UnionPayPaymentOpenService.java`
- [x] T067 [US4] 查单改经 Locator `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PaymentQueryServiceImpl.java`
- [x] T068 [US4] 超时关单前真实查单 `payflow-cashier-server/src/main/java/com/payflow/cashier/task/OrderTimeoutTask.java`
- [x] T069 [US4] MQ 消费超时路径改 Locator 查单 `payflow-cashier-server/src/main/java/com/payflow/cashier/consumer/OrderMqConsumer.java`
- [x] T070 [US4] 消除 `PayChannelServiceImpl` Setter 注入，改 `@Lazy` 构造器 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PayChannelServiceImpl.java`
- [x] T071 [US4] 修复订单超时 MQ 延迟级别 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/OrderMqProducerImpl.java`

**Checkpoint**: 无 `WxPayNativeHandler` 在 Service 层直接注入；admin/cashier JWT 仅一套实现

---

## Phase 7: User Story 5 - 可观测性与运维就绪 (Priority: P2)

**Goal**: recon 指标、日志脱敏、compose/Flyway 修复、healthcheck（FR-016~020）  
**Independent Test**: 见 `spec.md` US5；`quickstart.md` Wave 2 §8~9

**Contract**: `contracts/observability-ops-contract.md`

### Implementation for User Story 5

- [x] T072 [US5] recon-server 暴露 metrics/prometheus `payflow-recon-server/src/main/resources/application.yml`
- [x] T073 [US5] 注册对账自定义指标（duration/failures/diff.count）`payflow-recon-server/src/main/java/com/payflow/recon/config/ReconMetricsConfig.java`（新建）
- [x] T074 [P] [US5] cashier/recon 默认日志级别 prod=INFO `payflow-cashier-server/src/main/resources/application-prod.yml`
- [x] T075 [P] [US5] recon 默认日志级别 prod=INFO `payflow-recon-server/src/main/resources/application-prod.yml`
- [x] T076 [US5] 微信 HTTP 客户端响应 body 截断脱敏 `payflow-payment-channels/payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayV3HttpClient.java`
- [x] T077 [US5] 修复 compose MySQL 初始化（移除 `full-reseed` 死链）`docker-compose.yml`
- [x] T078 [US5] 新增 `scripts/docker-init/` 或 compose 挂载调用 `install_demo_db.py` `scripts/docker-init/README.md`
- [x] T079 [US5] 同步 Flyway history 至 V11 `scripts/install_demo_db.py`
- [x] T080 [P] [US5] 新增环境变量模板 `.env.example`
- [x] T081 [P] [US5] 三 Java 服务 compose healthcheck `docker-compose.yml`
- [x] T082 [US5] prod 启用 `flyway.validate-on-migrate: true` `payflow-admin-server/src/main/resources/application-prod.yml`

**Checkpoint**: `docker compose up` 一次性成功；`curl recon:3004/actuator/prometheus` 有 recon 指标

---

## Phase 8: User Story 6 - 国际化（i18n）真正落地 (Priority: P2)

**Goal**: admin ≥80% 主流程 UI 可切换语言；cashier 注册 i18n（FR-021~022）  
**Independent Test**: 见 `spec.md` US6；`quickstart.md` Wave 2 §10

### Implementation for User Story 6

- [x] T083 [P] [US6] 扩展 admin 英文文案：订单模块 `payflow-admin-client/src/locales/en-US.ts`
- [x] T084 [P] [US6] 扩展 admin 英文文案：商户/渠道 `payflow-admin-client/src/locales/en-US.ts`
- [x] T085 [P] [US6] 扩展 admin 英文文案：用户/角色 `payflow-admin-client/src/locales/en-US.ts`
- [x] T086 [P] [US6] 扩展 admin 英文文案：对账模块 `payflow-admin-client/src/locales/en-US.ts`
- [x] T087 [US6] 订单页改用 `$t` `payflow-admin-client/src/pages/admin/orders/index.vue`
- [x] T088 [P] [US6] 商户页改用 `$t` `payflow-admin-client/src/pages/admin/merchants.vue`
- [x] T089 [P] [US6] 渠道页改用 `$t` `payflow-admin-client/src/pages/admin/channels.vue`
- [x] T090 [US6] cashier 入口注册 `createI18n` `payflow-cashier-client/src/main.ts`
- [x] T091 [P] [US6] 扩展 cashier 中文/英文资源 `payflow-cashier-client/src/locales/zh-CN.ts`
- [x] T092 [P] [US6] PC/H5 收银台主文案 `$t` `payflow-cashier-client/src/pages/cashier/pc/index.vue`
- [x] T093 [US6] 通知相对时间跟随 i18n locale `payflow-admin-client/src/components/NotificationPopover.vue`

**Checkpoint**: admin 切 en-US 后主要 CRUD 表头为英文；cashier 收银台文案走 i18n

---

## Phase 9: User Story 7 - 核心质量门禁与测试加固 (Priority: P3)

**Goal**: 支付/对账核心单测、JaCoCo、E2E 进 CI（FR-023~025）  
**Independent Test**: 见 `spec.md` US7；`quickstart.md` Wave 3 §11~12

### Tests for User Story 7

- [x] T094 [P] [US7] 支付回调幂等/条件更新单元测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/PayNotifyServiceTest.java`
- [x] T095 [P] [US7] 微信验签失败/成功路径单元测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/sdk/wxpay/WxPaySignatureVerifierTest.java`
- [x] T096 [P] [US7] 对账比对四类 diff 测试（fixture CSV）`payflow-recon-server/src/test/java/com/payflow/recon/service/ReconCompareServiceTest.java`
- [x] T097 [P] [US7] 账单解析器冒烟测试 `payflow-recon-server/src/test/java/com/payflow/recon/parser/AlipayBillParserTest.java`
- [x] T098 [US7] `DashboardMetricsMapperTest` 改 `@EnabledIf` 或 Testcontainers，禁止静默 return `payflow-admin-server/src/test/java/com/payflow/admin/mapper/DashboardMetricsMapperTest.java`
- [x] T099 [P] [US7] 误标 `*IT` 类重命名为 `*Test`（如 `ReconAggregationServiceIT.java`）`payflow-admin-server/src/test/java/com/payflow/admin/service/recon/`

### Implementation for User Story 7

- [x] T100 [US7] 根 POM 配置 JaCoCo（初期门禁 40%）`pom.xml`
- [x] T101 [US7] CI 新增 E2E job（MySQL + seed + 三后端 + Playwright）`.github/workflows/ci.yml`
- [x] T102 [US7] Docker 构建前保留 `mvn test` 或 CI 分层验证（文档化 `-DskipTests` 仅用于镜像缓存层）`payflow-admin-server/Dockerfile`

**Checkpoint**: `mvn test` 通过且生成 JaCoCo 报告；CI E2E job 绿

---

## Phase 10: User Story 8 - 文档与配置一致性修正 (Priority: P3)

**Goal**: 架构文档与实现一致；消除死链；环境变量清单（FR-026~027）  
**Independent Test**: 见 `spec.md` US8；`quickstart.md` Wave 3 §13

### Implementation for User Story 8

- [x] T103 [US8] 重写对账架构说明（admin 直读 DB、recon 仅批处理）`docs/reconciliation.md`
- [x] T104 [P] [US8] 更新 `CLAUDE.md` 对账 Flow 小节（与 014 plan 一致）
- [x] T105 [P] [US8] 统一 SQL 初始化权威入口说明 `sql/README.md`
- [x] T106 [US8] 修复 recon 异常指引中的过时 SQL 路径 `payflow-recon-server/src/main/java/com/payflow/recon/exception/GlobalExceptionHandler.java`
- [x] T107 [US8] 确认 `docs/CONTRACT_MATRIX.md` 无因 `R` 统一导致的破坏性描述（必要时补充说明）

**Checkpoint**: 按更新后文档可完成一次环境搭建，无死链

---

## Phase 11: Polish & Cross-Cutting（Wave 验收与合入）

**Purpose**: 跨故事回归、合宪检查、合入前验证

- [x] T108 [P] 执行 `specs/014-product-optimization/quickstart.md` Wave 1 全量检查清单
- [x] T109 [P] 执行 `quickstart.md` Wave 2 检查清单（compose + i18n + 一致性）
- [x] T110 执行 `quickstart.md` Wave 3 检查清单（测试 + 文档）
- [x] T111 [P] Playwright 回归 admin 对账 + cashier 冒烟 `payflow-admin-client/e2e/`、`payflow-cashier-client/e2e/`
- [x] T112 三服务启动日志巡检：支付下单→回调→对账任务无 ERROR 阻断
- [x] T113 宪法合规自检（模块边界/渠道抽象/响应格式/密钥/分页/SQL/测试 DoD）`specs/014-product-optimization/plan.md` Constitution Check

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup (Phase 1)
  → Foundational (Phase 2) — 阻断所有 US
    → US1 (Phase 3) — 可与 US2/US3 并行（不同模块），但 US1 应优先合入
    → US2 (Phase 4) — 依赖 T006/T007（分页/批配置）
    → US3 (Phase 5) — 可与 US2 并行（前端）
    → US4 (Phase 6) — 建议在 US1 稳定后（触及 JWT/响应/查单）
    → US5 (Phase 7) — 可与 US4 并行
    → US6 (Phase 8) — 可与 US5 并行
    → US7 (Phase 9) — 依赖 US1~US4 核心实现完成
    → US8 (Phase 10) — 可与 US7 并行
    → Polish (Phase 11) — 依赖目标 Wave 内所有 US
```

### User Story Dependencies

| Story | 依赖 | 说明 |
|-------|------|------|
| US1 | Foundational | 需 `NotifyDedupService` |
| US2 | Foundational | 需 `PageRequest`、批大小配置 |
| US3 | 无硬依赖 US1/2 | 可独立前端交付 |
| US4 | US1 建议先合入 | 查单/响应改动面大 |
| US5 | 无 | 运维项独立 |
| US6 | 无 | i18n 独立 |
| US7 | US1 + US2 核心 | 测试覆盖实现点 |
| US8 | US5 compose 修复 | 文档描述 compose 行为 |

### Within Each User Story

- 后端：common → payment-core/channels → server
- 前端：api/request → composables → pages
- 数据库迁移先于实体 TypeHandler 启用

### Module Boundary Order

```text
payflow-common → payflow-payment-core → payflow-payment-channels/*
  → payflow-cashier-server / payflow-admin-server / payflow-recon-server
  → payflow-admin-client / payflow-cashier-client
```

---

## Parallel Execution Examples

### US1（支付安全）

```bash
# 可并行：
T011 WxPayNotifyHelper 验签
T016 EncryptedStringTypeHandler
T020 AuthController 登出
# 然后串行：
T012 → T013 → T014 → T015
```

### US2（性能）

```bash
# 可并行：
T023~T025 对账三服务批量写
T027~T029 分页上限
T030~T036 N+1 各项
T037~T039 日期查询
```

### US3（前端体验）

```bash
# 可并行：
T041~T046 admin P0 页面
T048~T049 cashier PC/H5
T053~T056 空状态批量
```

---

## Implementation Strategy

### MVP First（推荐：Wave 1 = US1 + US2 + US3）

1. Phase 1 Setup + Phase 2 Foundational
2. **US1** 支付安全（最高风险）
3. **US2** 性能（可用性）
4. **US3** 前端体验（运营/付款用户可感知）
5. **STOP & VALIDATE**：`quickstart.md` Wave 1 + Playwright 关键路径
6. 再进入 Wave 2（US4~US6）→ Wave 3（US7~US8）

### 最小切片 MVP（仅 US1）

若需极速止血：仅完成 Phase 1~2 + **US1（T011~T020）**，验证 SC-001/SC-002 后合入。

### Incremental Delivery

| 迭代 | 交付 | 验收 |
|------|------|------|
| Iteration 1 | US1 | 回调验签 + 幂等 + 加密 |
| Iteration 2 | US2 | 事务拆分 + 批量 + 分页 |
| Iteration 3 | US3 | 静默失败修复 |
| Iteration 4 | US4~US6 | 一致性 + 运维 + i18n |
| Iteration 5 | US7~US8 | 测试门禁 + 文档 |

---

## Task Summary

| Phase | Story | Task IDs | Count |
|-------|-------|----------|-------|
| 1 Setup | — | T001~T005 | 5 |
| 2 Foundational | — | T006~T010 | 5 |
| 3 US1 | P1 🎯 | T011~T020 | 10 |
| 4 US2 | P1 | T021~T040 | 20 |
| 5 US3 | P1 | T041~T056 | 16 |
| 6 US4 | P2 | T057~T071 | 15 |
| 7 US5 | P2 | T072~T082 | 11 |
| 8 US6 | P2 | T083~T093 | 11 |
| 9 US7 | P3 | T094~T102 | 9 |
| 10 US8 | P3 | T103~T107 | 5 |
| 11 Polish | — | T108~T113 | 6 |
| **Total** | | **T001~T113** | **113** |

**Parallelizable ([P])**: 约 52 项  
**Suggested MVP scope**: Phase 1~2 + US1（T001~T020）；完整 Wave 1 为 T001~T056  
**Format validation**: ✅ 全部任务符合 `- [ ] Txxx [P?] [USn?] Description with path` 格式
