# Implementation Plan: 产品质量优化与升级专项

**Branch**: `014-product-optimization` | **Date**: 2026-05-29 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/014-product-optimization/spec.md`

**Note**: 本模板由 `/speckit-plan` 命令填充。执行工作流见 `.specify/templates/plan-template.md`。

## Summary

本专项对 PonyFlux-Pay 进行**质量加固与工程化升级**，不新增对外业务功能。基于 2026-05-29 全栈审计（后端/前端/工程化），按 **Wave 1→2→3** 分波交付：

| Wave | 范围 | User Stories | 核心产出 |
|------|------|--------------|----------|
| **Wave 1（MVP）** | P1 | US1~US3 | 微信验签+回调幂等、下单事务拆分、对账批量写、分页上限、N+1 修复、关键页错误/空态 |
| **Wave 2** | P2 | US4~US6 | `R<T>`/`JwtService` 下沉 common、查单 SPI 统一、compose/Flyway 修复、recon 指标、i18n 分批落地 |
| **Wave 3** | P3 | US7~US8 | 支付/对账核心单测、JaCoCo 40%、E2E 进 CI、文档与 `.env.example` 同步 |

技术决策详见 [research.md](research.md)；结构变更见 [data-model.md](data-model.md)；行为契约见 [contracts/](contracts/)。

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.x
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Vue 3.4 / Element Plus / Pinia 2.1 / ECharts 5.5 / RocketMQ / Redis
**Storage**: MySQL（`payflow_admin` + `payflow_cashier` 双库）/ Redis
**Testing**: JUnit 5 + Mockito / Testcontainers（逐步引入）/ Playwright E2E
**Target Platform**: Linux Server (JVM 17) / Web Browser
**Project Type**: web-app（多模块支付网关 + 双前端 SPA）
**Performance Goals**: 200 并发下单渠道 2s 延迟下本地下单成功率 ≥99%；5 万条账单对账耗时下降 ≥60%；分页单次 ≤100 行
**Constraints**: 不新增业务表（可选 `bill_date` 列除外）；不引入新中间件；不横向扩展功能；迁移期允许响应格式双轨 1 迭代
**Scale/Scope**: 三后端 + 双前端；~39 个现有 Java 测试类；8 个 Playwright spec；27 条 FR

## Constitution Check

*GATE: Phase 0 研究前 — 已通过。Phase 1 设计后复查 — 已通过。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 | [x] PASS | `R<T>`、`JwtService`、`PageRequest` 下沉 `payflow-common`（共享基础设施）；业务逻辑仍留在各 server；渠道 Handler 不泄漏到 Service。 |
| 2 | II. 支付渠道抽象 | [x] PASS | 消除 `WxPayNativeHandler` 硬编码；查单/关单改 `PayChannelPaymentOpenService`；验签在 `payflow-payment-wechat` 内实现。 |
| 3 | III. 数据库分区 | [x] PASS | 无新业务表；可选 `cashier_payments.bill_date` 属 `cashier_` 前缀；对账表仍在 `recon_`。 |
| 4 | IV. API 响应规范 | [x] PASS | 统一 `R<T>`；迁移期双轨见 Complexity Tracking，T2 前完成。 |
| 5 | V. 密钥与配置安全 | [x] PASS | cashier `channelConfig` 加密；prod 日志脱敏；`.env.example`；验签失败 fail-close。 |
| 6 | 编码规范 | [x] PASS | 构造器注入；消除 setter 注入循环依赖（`PayChannelServiceImpl`）；中文 Javadoc。 |
| 7 | 数据库访问 | [x] PASS | 分页 maxLimit；批量写入；禁止 `DATE()` 索引失效模式；N+1 修复。 |
| 8 | 安全编码 | [x] PASS | 回调验签/防重放/幂等；登出黑名单不吞异常；参数校验保持。 |
| 9 | 测试规范 | [x] PASS（Wave 3） | Wave 1~2 以手工/E2E 验证为主；Wave 3 落地 JaCoCo 40% + E2E CI + 核心单测；80% 为长期目标。 |

**Gate Result**: ALL PASS（测试覆盖率门禁分 Wave 3 达成，已在 Assumptions 备案）

## Project Structure

### Documentation (this feature)

```text
specs/014-product-optimization/
├── plan.md              # 本文件
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/           # Phase 1 行为契约
│   ├── shared-platform-contract.md
│   ├── payment-security-contract.md
│   ├── frontend-ux-contract.md
│   └── observability-ops-contract.md
└── tasks.md             # Phase 2（/speckit-tasks 生成）
```

### Source Code (repository root)

```text
# Wave 1 — 支付安全与性能
payflow-payment-channels/payflow-payment-wechat/
├── WxPayNotifyHelper.java              # [修改] RSA 验签 + 防重放
payflow-cashier-server/
├── service/impl/PaymentServiceImpl.java    # [修改] 事务拆分
├── service/PayNotifyService.java           # [修改] 幂等 + 条件更新
├── entity/PayChannelAccount.java           # [修改] channelConfig 加密
├── typehandler/EncryptedStringTypeHandler  # [新增/复用]
payflow-recon-server/
├── service/ReconCompareService.java        # [修改] 批量 diff
├── service/ReconExecuteService.java        # [修改] 批量 bill insert
├── service/ReconDiffHealService.java       # [修改] 批量 update
payflow-admin-server/
├── service/impl/MerchantNotifyQueryServiceImpl.java  # [修改] N+1
├── service/impl/PaymentAccountServiceImpl.java       # [修改] JOIN
├── controller/PaymentAccountController.java          # [修改] 真分页
payflow-admin-client/
├── pages/admin/reconcile/work-item-detail.vue  # [修改] loading/error
├── components/NotificationPopover.vue          # [修改] 错误提示
payflow-cashier-client/
├── composables/useCashierCheckout.ts           # [修改] loading 复位
├── pages/pc/index.vue, pages/h5/index.vue    # [修改] error 态

# Wave 2 — 一致性 + 运维 + i18n
payflow-common/
├── web/R.java, PageRequest.java              # [新增/迁入]
├── security/JwtService.java                  # [新增/合并]
payflow-payment-core/
├── PayChannelPaymentOpenService.java         # [扩展] queryOrder
payflow-cashier-server/
├── service/impl/PaymentQueryServiceImpl.java # [修改] Locator
├── task/OrderTimeoutTask.java                # [修改] 真实查单
payflow-recon-server/
├── application.yml                           # [修改] metrics/prometheus
docker-compose.yml, .env.example              # [修改/新增]
scripts/install_demo_db.py                    # [修改] Flyway V11
payflow-admin-client/src/locales/             # [扩展] i18n keys
payflow-cashier-client/src/main.ts            # [修改] 注册 i18n

# Wave 3 — 测试 + 文档
pom.xml                                       # [修改] JaCoCo
.github/workflows/ci.yml                      # [修改] e2e job
payflow-cashier-server/src/test/              # [新增] PaymentNotify 等
payflow-recon-server/src/test/                # [新增] ReconCompare 等
docs/reconciliation.md, CLAUDE.md, sql/README.md  # [修改]
```

**Structure Decision**: 不新增 Maven 模块。共享能力收敛至 `payflow-common`；渠道验签留在 `payflow-payment-wechat`；对账批处理留在 `payflow-recon-server`；管理端 N+1/分页在 `payflow-admin-server`；前端体验在双 client。

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 响应格式迁移期双轨（admin ApiResponse + common R） | 一次性切换会破坏 26+ Controller 与前端契约 | 直接删除 ApiResponse 无回归测试（Wave 3 前风险过高） |
| 对账分页上限 500 vs 通用 100 | 对账工单列表运营需较大页 | 统一 100 会导致对账员频繁翻页，与 013 产品约定冲突 |
| JaCoCo 40% 而非宪法 80% | 存量测试基线极低，一步 80% 导致 exclude 泛滥 | 假绿比低阈值更损害质量（research Decision 14） |

## Phase 0 Output

✅ [research.md](research.md) — 15 项决策，无未决 NEEDS CLARIFICATION

## Phase 1 Output

✅ [data-model.md](data-model.md) — 结构变更与运行时实体  
✅ [contracts/](contracts/) — 4 份行为契约  
✅ [quickstart.md](quickstart.md) — 分 Wave 验证步骤  
✅ CLAUDE.md agent context 已更新指向本 plan

## Phase 2 Preview（供 /speckit-tasks 参考）

任务生成建议按 Wave 切分 Epic：

1. **EPIC-W1-Security**: FR-001~004（微信验签、幂等、加密）
2. **EPIC-W1-Perf**: FR-005~009（事务拆分、批量、分页、N+1、日期查询）
3. **EPIC-W1-UX**: FR-010~012（前端错误/空态/loading）
4. **EPIC-W2-Platform**: FR-013~015（R/Jwt/SPI）
5. **EPIC-W2-Ops**: FR-016~020（metrics、日志、compose、Flyway）
6. **EPIC-W2-i18n**: FR-021~022
7. **EPIC-W3-Quality**: FR-023~027（测试、文档）

每项任务应映射 FR 编号并引用对应 contract 文件。
