# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 语言约定

所有生成的文本输出（描述、注释、提交信息、文档等）必须使用中文，但以下情况保留原文：
- 专业术语（如 Strategy Pattern、JWT、HMAC-SHA256、Spring Boot 等）
- 代码标识符（类名、方法名、变量名、包名）
- 技术栈名称（MyBatis-Plus、Redis、RocketMQ、Vue 等）
- 代码块和配置示例

## Build & Dev Commands

```bash
# Build all Maven modules (skip tests)
mvn -B -DskipTests compile

# Build with tests
mvn -B clean package

# Build a single module
mvn -B -pl payflow-common compile

# Run a specific server (Spring Boot)
mvn -B -pl payflow-admin-server spring-boot:run
mvn -B -pl payflow-cashier-server spring-boot:run
mvn -B -pl payflow-recon-server spring-boot:run

# Frontend E2E validation (local Playwright/Playwright CLI already available)
# After UI/payment/admin-flow changes: run Playwright as appropriate, monitor backend logs,
# fix issues from logs, then repeat validation until key flows pass without blocking backend errors
cd payflow-admin-client && npx playwright test
cd payflow-cashier-client && npx playwright test

# Reset demo database (schema + seed)
python scripts/install_demo_db.py

# Verify admin passwords after reseed
python scripts/verify_admin_password.py
```

## Architecture Overview

**PonyFlux-Pay** is a payment gateway system — Java 17, Spring Boot 3.2.5, Maven multi-module. MyBatis-Plus 3.5.7 for ORM, RocketMQ for async messaging, Redis for caching, XXL-Job for scheduled tasks.

### Module Map

| Module | Purpose |
|--------|---------|
| `payflow-common` | Shared: `BizException`, `AesEncryptor` (AES-256-GCM), `RedisTopics` constants |
| `payflow-payment-core` | Payment SPI: `PayStrategy` interface, `PayMethod` enum, DTOs (`PayResult`, `RefundResult`, `NotifyResult`), `ChannelConfigHolder` |
| `payflow-payment-channels` | Aggregator POM for all payment channel submodules |
| `payflow-payment-channels/payflow-payment-wechat` | WeChat Pay handlers (Native/H5/App/JSAPI/Mini/MicroPay) |
| `payflow-payment-channels/payflow-payment-alipay` | Alipay handlers (QR/WAP/App/Face), `AliPayClientCache` |
| `payflow-payment-channels/payflow-payment-union` | UnionPay handlers (H5/QR/refund/bill download), RSA-SHA256 signing, HTTP client |
| `payflow-cashier-server` | **Merchant-facing payment service** (port 3002): order creation, payment routing, refunds, callbacks |
| `payflow-admin-server` | **Admin management backend** (port 3003): dashboard, merchant/channel config, reconciliation UI, RBAC |
| `payflow-recon-server` | **Reconciliation engine** (port 3004): bill download → parse → compare → diff healing |
| `payflow-sdk-java` | Lightweight HMAC-SHA256 signer for external merchants |

### Dual MySQL Database Design

- **`payflow_admin`** — operational config: merchants, channels, accounts, routes, users, roles, menus, recon tables (`recon_*`)
- **`payflow_cashier`** — transactional data: orders, payments, refunds (`cashier_*` prefix)

Admin-server uses **two datasources** manually configured (no `@MapperScan` on the primary — each MapperFactoryBean is a `@Bean` to avoid clashes). Cashier-server uses a single datasource + H2 for runtime fallback.

### Payment Routing (Strategy Pattern)

1. Merchant requests payment → `PaymentServiceImpl` routes via `PayChannelService.routeToAccount()` to select a `PayChannelAccount`
2. `PayStrategyLocator` resolves the strategy bean by name (`{code_lowercase}PayStrategy`, e.g. `wechat_nativePayStrategy`)
3. Each strategy delegates to a channel handler from `payflow-payment-channels/payflow-payment-*` module (e.g. `WxPayNativeHandler`)
4. `PayStrategyRegistry` (built from Spring-injected `List<PayStrategy>`) also auto-detects channels for notify parsing
5. Notify handling is separated: `{channel}OpenService` (implements `PayChannelOpenService`) handles async callbacks, `{channel}PaymentOpenService` (implements `PayChannelPaymentOpenService`) handles payment operations

### Auth Mechanisms

- **Admin-server**: JWT interceptor (`JwtInterceptor`) on `/api/v1/admin/**` via `Authorization: Bearer <token>`. HS256, 24h expiry. Login + captcha excluded. Rate limiting on login (5 failures → 900s lock).
- **Cashier-server**: Three layers — (1) `MerchantSignatureInterceptor` (HMAC-SHA256 + timestamp + nonce) on merchant endpoints, (2) `PaymentIdempotencyInterceptor` on POST payments, (3) `JwtAuthInterceptor` on order endpoints.
- **Internal service communication**: `X-Payflow-Internal-Token` header shared between admin-server and recon-server.

### Reconciliation Flow

职责划分见 [`docs/reconciliation.md`](docs/reconciliation.md)：

| 组件 | 职责 |
|------|------|
| **recon-server** | 批处理：下载账单 → 解析 → 比对 → 差异标注（无管理 UI API） |
| **admin-server** | 运营控制台：直读 `recon_*` 与 cashier 报表；差异工单 / SLA / 报告订阅 |
| **cashier-server** | 交易源：`cashier_payments` / `cashier_orders` |

**recon-server 状态机**：`INIT` → `DOWNLOADING` → `PARSING` → `COMPARING` → `SUCCESS`/`FAIL`

1. `ReconTaskSeedService` 按账户 + 账单日生成 T-1 任务
2. 渠道账单下载 → `recon_bill_record` 批量入库（默认 batch-size 500）
3. `ReconCompareService` 与 `cashier_payments` 半开区间 `[dayStart, dayEnd)` 比对 → `recon_diff`（`CHANNEL_ONLY` / `LOCAL_ONLY` / `AMOUNT_MISMATCH` / `STATUS_MISMATCH`）
4. `ReconDiffHealService` 建议修复动作（不自动愈合）

业务错误码：**7500-7599** 预留给对账。

### API Response Convention

三后端统一使用 `com.payflow.common.web.R<T>`：

```
{ "code": 0, "message": "success", "data": { "..." } }
```

部分 admin Controller 仍返回 `Map<String,Object>`，结构与 `R` 字段一致。前端 Axios 拦截器自动解包 `data`。

### Frontend Stack

Both clients: Vue 3.4 + TypeScript + Vite 5 + TailwindCSS 3.4 + Element Plus + Axios.
- **admin-client**: Pinia 2.1, ECharts 5.5, Vue I18n (zh-CN/en)
- **cashier-client**: Pinia 2.1, focused SPA (cashier page, login, receipt)

Full frontend ↔ backend contract documented in [`docs/CONTRACT_MATRIX.md`](docs/CONTRACT_MATRIX.md).

### Key Documentation Files

| File | Contents |
|------|----------|
| `docs/CONTRACT_MATRIX.md` | Complete frontend-to-backend API mapping |
| `docs/reconciliation.md` | Recon architecture, tables, flow, error codes |
| `docs/REFUND_STATE_MACHINE.md` | Refund states and approval process |
| `docs/optimization-full-report.md` | Full system optimization audit (2026-05) |
| `sql/schema/` | Full DDL per database (`payflow_admin`, `payflow_cashier`) |
| `sql/seed/` | Realistic demo data for all admin pages |
| `sql/migrations/` | Historical Flyway-style incremental migrations (reference) |
| `scripts/install_demo_db.py` | One-shot install: schema + seed |

### Port Assignments

- `payflow-cashier-server`: **3002**
- `payflow-admin-server`: **3003**
- `payflow-recon-server`: **3004**
- `payflow-admin-client` (dev): **3001**
- `payflow-cashier-client` (dev): **5173**

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at specs/015-cashier-i18n/plan.md
<!-- SPECKIT END -->
