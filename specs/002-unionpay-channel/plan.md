# Implementation Plan: 银联支付渠道完整接入

**Branch**: `002-unionpay-channel` | **Date**: 2026-05-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-unionpay-channel/spec.md`

## Summary

完善银联/云闪付支付渠道从占位到完整实现：接入银联开放平台网关，实现 H5 支付、扫码支付(QR)、退款、异步通知验签和 T-1 对账全流程。同时重构支付渠道 Maven 模块结构，创建 `payflow-payment-channels` 父级聚合模块，将 wechat/alipay/union 三个渠道模块统一收纳，并同步更新所有依赖引用。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.2.5, MyBatis-Plus 3.5.7, Hutool 5.8.26, Lombok
**Storage**: MySQL 8.x (`payflow_cashier` + `payflow_admin` 双库)
**Testing**: JUnit 5 + Spring Boot Test + Maven Surefire
**Target Platform**: Linux server (开发环境 Windows)
**Project Type**: Web service (Maven 多模块聚合项目)
**Performance Goals**: 下单响应 < 3s (SC-002), 日均 1000+ 笔 (SC-008), 通知处理成功率 >= 99.5% (SC-003)
**Constraints**: 双数据源不可混用; 金额用 Long(分); API 响应必须 `{code, message, data}`; 密钥 AES-256-GCM 加密
**Scale/Scope**: 1 个新支付渠道, 2 种支付方式(H5+QR), 3 个模块重构(wechat/alipay 迁入父模块)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原则 | 状态 | 验证 |
|------|------|------|
| **I. 模块边界纪律** | PASS | Union 模块仅依赖 `payflow-payment-core`，不引用 cashier/admin 实体。策略 Bean 放在 `payflow-cashier-server` 的 `sdk/strategy/`。对账实现放在 `payflow-recon-server`。父 POM `payflow-payment-channels` 仅作为聚合模块，不写业务代码。 |
| **II. 支付渠道抽象** | PASS | `UnionH5Strategy` + `UnionQrStrategy` 实现 `PayStrategy` 接口，Bean 命名为 `union_h5PayStrategy` / `union_qrPayStrategy`。`UnionPayPaymentOpenService` 实现 `PayChannelPaymentOpenService`。配置通过 `ChannelConfigHolder` 传递。 |
| **III. 数据库分区** | PASS | 交易数据(`cashier_payments`/`cashier_refunds`)存 cashier 库，对账数据(`recon_*`)存 admin 库。不对现有 schema 做破坏性变更。 |
| **IV. API 响应规范** | PASS | 所有 Controller 返回 `{code, message, data}` 格式，复用现有 `R<T>` 包装类。 |
| **V. 密钥与配置安全** | PASS | 银联商户密钥/证书密码入库前 AES-256-GCM 加密，证书文件存储沿用现有方案。 |

**Constitution Check Result: ALL PASSED** — 无违规项。

## Project Structure

### Documentation (this feature)

```text
specs/002-unionpay-channel/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── unionpay-api.md  # UnionPay API interface contract
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root) — 目标结构

```text
pom.xml                                  # modules 区段更新
├── payflow-payment-channels/            # NEW: 渠道聚合父 POM
│   ├── pom.xml                          # packaging=pom, 聚合 3 个子模块
│   ├── payflow-payment-wechat/          # MOVED: 从根目录迁入 (git mv)
│   ├── payflow-payment-alipay/          # MOVED: 从根目录迁入 (git mv)
│   └── payflow-payment-union/           # MOVED + 完整实现
│       ├── pom.xml                      # 新增银联 SDK/HTTP 依赖
│       └── src/main/java/com/payflow/payment/union/
│           ├── UnionPayIntegration.java   # REWRITE: 常量和工具方法
│           ├── UnionPayConfigLoader.java  # NEW: ChannelConfig JSON 解析
│           ├── UnionPayAccountConfig.java # NEW: 配置 POJO (商户号/证书/密钥)
│           ├── UnionPayH5Handler.java     # NEW: H5 支付网关交互
│           ├── UnionPayQrHandler.java     # NEW: QR 支付网关交互
│           ├── UnionPayNotifyHelper.java  # NEW: 异步通知验签
│           ├── UnionPayBillService.java   # NEW: 账单下载 API
│           └── UnionPayApiConstants.java  # NEW: 网关 URL / 接口路径常量
├── payflow-payment-core/
│   └── PayMethod.java                   # UPDATE: 新增 UNION_QR 枚举值
├── payflow-cashier-server/
│   └── src/main/java/com/payflow/cashier/
│       ├── sdk/strategy/
│       │   ├── UnionH5Strategy.java       # REWRITE: 占位URL → 真实API调用
│       │   └── UnionQrStrategy.java       # NEW: QR 策略
│       ├── sdk/unionpay/
│       │   └── UnionPayCashierHelper.java # NEW: 渠道级辅助(签名/验签/HTTP)
│       ├── openservice/payment/impl/
│       │   └── UnionPayPaymentOpenService.java  # REWRITE: 支持 QR + 退款实现
│       └── service/impl/
│           └── RefundServiceImpl.java       # FIX: normalizeChannelCode 添加 "unionpay"
├── payflow-recon-server/
│   └── src/main/java/com/payflow/recon/
│       ├── openservice/bill/impl/
│       │   └── UnionpayReconChannelOpenService.java  # NEW: 账单下载+解析调度
│       └── parser/impl/
│           └── UnionpayBillParser.java      # NEW: 银联CSV账单解析
├── payflow-admin-server/
│   └── src/main/resources/
│       └── sql/migrations/2026-05-12_unionpay-qr-method-seed.sql  # NEW: payment_methods 种子
├── sql/
│   └── full-reseed-payflow-demo.sql        # UPDATE: 新增 UNION_QR 种子数据
└── docs/
    └── CONTRACT_MATRIX.md                  # UPDATE: 新增银联接口映射
```

**Structure Decision**: 本项目为前后端分离的 Java Web 应用。后端采用 Maven 多模块聚合，前端为 Vue 3 + Vite 独立工程。本次变更聚焦后端模块重构和银联渠道实现。

## Complexity Tracking

> 无宪法违规项，无需填写。

---

## Phase 0: Research

### 研究结论摘要

| 研究项 | 决策 | 依据 |
|--------|------|------|
| 银联 SDK 选型 | 自实现 HTTP 签名客户端 | 参照 wechatpay-java 模式; 官方 SDK 过重且与 Spring Boot 3.x 兼容性存疑 |
| 银联 API 版本 | 银联全渠道 v5.x (ACP SDK 对应接口) | 最广泛使用的银联线上支付网关; H5/QR 均有标准接口 |
| 签名算法 | RSA-SHA256 (商户私钥签名) + 银联公钥验签 | 银联标准签名方案, 与现有证书管理兼容 |
| 支付接口类型 | H5: `frontUrl` 跳转; QR: `QRCode` 返回二维码图片/URL | 银联全渠道标准产品 |
| 通知验签 | 解析 `respMsg` 中的 `respCode` + 验签 `signature` | 银联异步通知为 HTTP POST, body 含签名字段 |
| Maven 迁移方式 | `git mv` 保留历史 + 创建新父 POM | 确保 git blame/log 不丢失 |
| 对账账单格式 | CSV (银联标准格式, 含交易流水号/金额/时间/手续费) | 与 AlipayBillParser 模式一致 |

详细研究见 [research.md](./research.md)。

---

## Phase 1: Design & Contracts

### 1. 数据模型变更

| 变更类型 | 表/实体 | 详情 |
|----------|---------|------|
| 新增枚举值 | `PayMethod` 枚举 | `UNION_QR("UNION_QR", "银联扫码支付")` |
| 新增种子数据 | `payment_methods` (admin 库) | `UNION_QR` 记录, method_code=UNION_QR, 关联 UNION 渠道 |
| 新增实体 | `UnionPayAccountConfig` | POJO: merId, signCertPath, signKeyPath, encryptCertPath, gatewayUrl, notifyUrl |
| 扩展 JSON | `cashier_channel_accounts.channel_config` | 存储 UnionPayAccountConfig 序列化 JSON |
| 复用表 | `cashier_payments` / `cashier_refunds` | 无 schema 变更, pay_method 字段存 "UNION_H5"/"UNION_QR" |
| 复用表 | `recon_bill_record` / `recon_diff` | 无 schema 变更, channel_code = "unionpay" |

完整数据模型见 [data-model.md](./data-model.md)。

### 2. 接口契约

银联渠道对外暴露的接口契约:

| 方向 | 端点 | 用途 |
|------|------|------|
| IN (给前端) | `POST /api/v1/payment/create` | 已有; payChannel=UNION_PAY, payMethod=UNION_H5/UNION_QR |
| IN (给银联) | `POST /notify/unionpay` | 已有路由; 需实现 UnionPayNotifyHelper 验签 |
| IN (给商户) | `POST /api/v1/merchant/refund` | 已有; channelCode=unionpay 需在 normalizeChannelCode 中添加 |
| OUT (调银联) | 银联全渠道网关 | H5 下单 / QR 下单 / 退款 / 账单下载 |

完整契约见 [contracts/unionpay-api.md](./contracts/unionpay-api.md)。

### 3. 快速开始

见 [quickstart.md](./quickstart.md)

---

## Phase 2: 任务分解概览

> 详细任务列表由 `/speckit-tasks` 生成到 `tasks.md`。

按依赖关系编排为 6 个阶段:

| 阶段 | 内容 | 优先级 | 预计文件数 |
|------|------|--------|-----------|
| 2.1 模块重构 | 创建 channels 父 POM, git mv 3 个模块, 更新根POM和所有依赖引用 | P1 | ~6 |
| 2.2 核心支付 | PayMethod 枚举 + Union API handlers + H5/QR Strategy 实现 | P1 | ~8 |
| 2.3 退款 | OpenService 退款实现, RefundServiceImpl normalizeChannelCode 修复 | P2 | ~2 |
| 2.4 通知 | UnionPayNotifyHelper, PayNotifyController 适配 | P2 | ~3 |
| 2.5 对账 | UnionpayReconChannelOpenService, UnionpayBillParser | P3 | ~3 |
| 2.6 集成 | SQL 种子, Admin UI, CONTRACT_MATRIX, 端到端验证 | P1-P3 | ~4 |

---

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 银联沙箱环境不稳定 | 阻塞开发测试 | 开发阶段使用 Mock 模式; 沙箱仅用于最终集成验证 |
| 模块迁移导致 Maven reactor 失败 | 阻塞所有渠道编译 | 增量迁移: 先验证 channels 父 POM 独立编译, 再更新根 POM |
| 银联 API 签名字段命名差异 | 不同接口签名字段名可能不同 | 参考官方文档逐接口适配; 统一签名工具方法 |
| 证书格式兼容性 | 银联使用 .pfx 格式需 JKS 转换 | 调研阶段确定; 优先使用 Java 原生 KeyStore 加载 |
