# PonyFlux-Pay 项目宪法

<!--
  Sync Impact Report
  ==================
  Version change: 0.0.0 (模板占位符) → 1.0.0 (初始宪法)
  Modified principles: N/A (首次正式版本)
  Added sections:
    - 核心原则 (5条)
    - 技术约束 (5节)
    - 开发工作流 (3节)
    - 治理
  Removed sections: 无
  Templates requiring updates:
    - .specify/templates/plan-template.md ✅ 无需修改 (Constitution Check 门禁自动填充)
    - .specify/templates/spec-template.md ✅ 无需修改
    - .specify/templates/tasks-template.md ✅ 无需修改
    - .specify/templates/checklist-template.md ✅ 无需修改
  Follow-up TODOs: 无
-->

## 核心原则

### I. 模块边界纪律（不可协商）

所有代码必须放在正确的 Maven 模块中。9 个模块有严格边界：

| 模块 | 职责范围 | 禁止行为 |
|------|----------|----------|
| `payflow-common` | 仅限共享工具、异常（`BizException`）、加密（`AesEncryptor`）、常量 | 禁止放 Spring Bean、业务逻辑、实体类 |
| `payflow-payment-core` | 支付 SPI：`PayStrategy` 接口、`PayMethod` 枚举、DTO。零 Spring 依赖 | 禁止加入渠道特有逻辑 |
| `payflow-payment-wechat/alipay/union` | 各渠道 API 处理器，实现 core 接口 | 禁止直接引用 cashier/admin 的实体类，必须通过 `ChannelConfigHolder` 传递配置 |
| `payflow-cashier-server` | 商户端支付处理、订单管理 | 禁止放管理后台逻辑 |
| `payflow-admin-server` | 后台运营管理、商户配置、对账 UI | 禁止放支付处理逻辑 |
| `payflow-recon-server` | 仅对账引擎（账单下载/解析/比对） | 禁止放管理后台 UI 逻辑；admin-server 通过内部 API 代理访问 |
| `payflow-sdk-java` | 轻量 HMAC-SHA256 签名工具，零依赖 | 禁止引入 Spring 或数据库依赖 |

**理由**：模块边界防止循环依赖，确保各模块可独立测试和部署。违反此原则曾导致 P0-04（RefundServiceImpl 硬编码渠道依赖）。

### II. 支付渠道抽象

所有支付渠道交互必须通过策略模式：

1. `PayStrategyRegistry`（由 Spring 注入的 `List<PayStrategy>` 构建）按 `PayMethod` 映射策略。
2. `PayChannelPaymentOpenServiceLocator` 按渠道代码定位服务。
3. 各策略将调用委托给 `payflow-payment-*` 模块的 Handler。

**规则**：
- 禁止在 Service 中直接注入具体的渠道 Handler（如 `WxPayNativeHandler`、`AliPayQrHandler`），必须使用 Locator/Registry。
- 新增支付渠道必须实现 `PayStrategy`，并注册为 Spring Bean，命名为 `{payMethodCode小写}PayStrategy`（如 `wechat_nativePayStrategy`）。
- 渠道配置必须通过 `ChannelConfigHolder.getChannelConfig()`（JSON 字符串）传递，支付模块禁止直接引用实体类。

**理由**：直接注入渠道 Handler（P0-04）造成硬编码依赖，新增或移除支付渠道需要修改业务代码。

### III. 数据库分区（不可协商）

系统使用两个 MySQL 数据库，表归属严格分离：

| 数据库 | 用途 | 表前缀 | 示例表 |
|--------|------|--------|--------|
| `payflow_admin` | 运营配置 | `admin_` | `admin_merchants`、`admin_channels`、`admin_users`、`admin_roles` |
| `payflow_admin` | 对账数据 | `recon_` | `recon_task`、`recon_bill_record`、`recon_diff` |
| `payflow_cashier` | 交易数据 | `cashier_` | `cashier_orders`、`cashier_payments`、`cashier_refunds` |

**规则**：
- admin-server 使用双数据源。主数据源（admin）必须使用手动 `MapperFactoryBean` 注册——禁止在主数据源上使用 `@MapperScan`。
- admin-server 中的 cashier mapper 必须放在 `mapper.cashier` 子包，并使用限定包的 `@MapperScan`。
- 新建实体必须遵循对应数据库的前缀约定。
- 禁止在应用层做跨库 JOIN 或关联查询，使用分开的查询。

**理由**：双数据源配置容易出错。违反 MapperFactoryBean 模式（参见 `docs/mybatis-multi-datasource-fix_2026-04-21.md`）会导致 MyBatis-Plus 自动配置冲突。

### IV. API 响应规范（不可协商）

所有后端控制器必须返回统一格式：

```json
{ "code": 0, "message": "success", "data": { ... } }
```

**规则**：
- `code = 0` 表示成功。非零为错误码（对账模块：7500-7599）。
- 使用 `Map<String, Object>` 或 `payflow-common` 中的 `R<T>` 包装类。
- 禁止在各模块中自创响应包装类。
- 前端 Axios 拦截器会自动解包 `data`——修改响应结构将破坏所有前端代码。
- 全局异常处理器必须返回此格式。禁止将堆栈信息或内部错误信息泄露到前端（参见 P0-03）。

**理由**：系统中已存在两套响应包装（cashier 的 `R<T>` 与 admin 的 `ApiResponse`——P1-02）。统一格式防止进一步碎片化。

### V. 密钥与配置安全

**规则**：
- 商户 API 密钥（`mch_key`、`app_secret`、`cert_password`）入库前必须使用 `AesEncryptor`（AES-256-GCM）加密存储。
- 主密钥必须通过环境变量或外部配置（`payflow.crypto.master-key`）注入，禁止硬编码。
- JWT 密钥必须从配置文件（application-remote.yml）读取，禁止使用硬编码默认值。`JwtProperties` 中的硬编码默认值仅限开发环境。
- CORS `allowedOrigins` 必须使用显式白名单，禁止 `"*"` 与 `allowCredentials(true)` 同时使用。
- 内部服务令牌（`X-Payflow-Internal-Token`）在生产环境必须可轮换。

**理由**：优化报告中确认 P0-01（CORS 通配符 + 凭证）和 P0-02（数据库密钥明文）为严重安全漏洞。

---

## 技术约束

### 技术栈（锁定）

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 构建 | Maven | （父 POM 统一管理） |
| 前端框架 | Vue 3 + TypeScript | 3.4 |
| 前端构建 | Vite | 5 |
| UI 组件库 | Element Plus | 2.5+ |
| 前端状态管理 | Pinia | 2.1 |
| 前端 HTTP | Axios | 1.6 |
| 消息队列 | RocketMQ | （可选，默认关闭） |
| 缓存 | Redis | — |
| 任务调度 | XXL-Job | 2.4.1（可选，默认关闭） |
| API 文档 | SpringDoc OpenAPI | 2.5.0 |

**规则**：
- 禁止引入替代的 ORM 框架、构建工具或前端框架。
- 可选依赖（RocketMQ、XXL-Job）必须使用 `@ConditionalOnProperty` 做特性开关，开发环境默认关闭。
- `admin-client` 和 `cashier-client` 的公共前端依赖（Vue、Element Plus、Axios）版本必须保持一致。

### 端口分配

| 服务 | 端口 |
|------|------|
| `payflow-cashier-server` | 3002 |
| `payflow-admin-server` | 3003 |
| `payflow-recon-server` | 3004 |
| `payflow-admin-client`（开发） | 3001 |
| `payflow-cashier-client`（开发） | 5173 |

修改端口时必须同步更新所有跨服务 URL 配置和 CORS 设置。

### 实体与 DTO 分离

- **实体类**（`entity/` 包）：通过 MyBatis-Plus `@TableName` 映射数据表，禁止直接在 Controller 响应中暴露。
- **DTO**（`dto/` 包）：API 契约，仅定义前端需要的字段。
- **Service 层**负责 Entity ↔ DTO 转换。Controller 只接收和返回 DTO。

### 前端 API 集成

- Axios `baseURL` = `/api/v1`。所有前端 API 调用使用相对路径。
- `docs/CONTRACT_MATRIX.md` 是前端方法到后端接口的权威映射表。新增接口必须同步更新。
- 前端 API 模块（`api/` 目录）必须对应后端 Controller 分组。
- 响应解包（从 `{ code, message, data }` 中提取 `data`）在 Axios 拦截器中完成，调用方只接收 `data` 载荷。

### 金额约定

所有金额以 **Long 型整数（分/fen）** 存储和传输。风控阈值和金额比较必须直接用分，禁止使用浮点类型做元/分转换。用 `BigDecimal` 做元分换算存在精度风险（P1-03）。

---

## 开发工作流

### 新增支付渠道的标准流程

1. 创建 `payflow-payment-{channel}` 模块：
   - 实现与渠道 API 交互的 Handler 类（如 `XxxPayHandler`）
   - 实现读取 `ChannelConfigHolder` JSON 的配置加载器
2. 在 `payflow-cashier-server` 中：
   - 创建实现 `PayStrategy` 的策略 Bean，命名为 `{payMethodCode小写}PayStrategy`
   - 在 `openservice.payment.impl` 中创建 `PayChannelPaymentOpenService` 实现
3. 将渠道种子数据添加到 `sql/full-reseed-payflow-demo.sql`
4. 在 `payflow-admin-client` 中新增渠道配置页面
5. 如果渠道提供账单下载，在 `payflow-recon-server` 中新增账单解析器
6. 更新 `docs/CONTRACT_MATRIX.md`

### 数据库迁移规范

1. 在 `sql/migrations/` 中创建时间戳命名的 SQL 文件（格式：`YYYY-MM-DD_描述.sql`）
2. Schema 变更必须向后兼容（增量式）。避免使用 `DROP COLUMN` 或 `RENAME TABLE`，如需重命名必须同时创建向后兼容视图。
3. 更新 `sql/full-reseed-payflow-demo.sql` 以反映新 Schema，确保全新安装不受影响。
4. 在相关架构文档中记录新表和字段。

### 错误码范围

| 范围 | 模块 |
|------|------|
| 0 | 成功 |
| 1xxx | 管理后台 / 认证 |
| 2xxx | 收银台 / 支付 |
| 3xxx | 订单 |
| 4xxx | 退款 |
| 5xxx | 商户 |
| 6xxx | 渠道 / 路由 |
| 7500–7599 | 对账 |

新增错误码必须使用对应模块的范围，禁止复用已有错误码。

---

## 治理

### 修订流程

1. 提议的变更必须附带理由和影响分析文档。
2. 对"不可协商"原则的修改需要 MAJOR 版本号递增，并全面审查所有关联模板和文档。
3. 新增原则或章节需要 MINOR 版本号递增。
4. 文字澄清和措辞修正需要 PATCH 版本号递增。

### 版本规则

本宪法遵循语义化版本（MAJOR.MINOR.PATCH）：
- **MAJOR**：向后不兼容的治理变更、删除或重新定义原则。
- **MINOR**：新增原则或章节、实质性扩展指导内容。
- **PATCH**：澄清、错别字修正、非语义优化。

### 合规审查

- 所有功能计划（`plan.md`）在进入 Phase 0 研究前，必须通过宪法检查（Constitution Check）门禁。
- Code Review 必须验证：模块边界纪律（原则 I）、支付渠道抽象（原则 II）、API 响应格式（原则 IV）。
- 数据库 Schema 变更必须对照分区规则（原则 III）进行验证。
- `CLAUDE.md` 文件在运行时开发指导上优先于本宪法。两者如有冲突，必须通过修订其中一份来解决。

**版本**: 1.0.0 | **批准日期**: 2026-05-10 | **最后修订**: 2026-05-10
