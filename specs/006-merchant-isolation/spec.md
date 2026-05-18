# Feature Specification: 商户数据隔离与水平越权急修（Phase 0）

**Feature Branch**: `006-merchant-isolation`
**Created**: 2026-05-18
**Status**: Draft
**Input**: User description: "商户隔离与水平越权急修：强制 JWT 上下文优先 + 资源所有权拦截器 + MyBatis MerchantScope 拦截器，堵漏 OrderController createOrder/getOrderDetail 等水平越权漏洞，建立商户数据隔离的纵深防御体系"

## Clarifications

### Session 2026-05-18

- Q: 本特性相关文档应使用何种语言？ → A: **全部使用中文**。包括 `spec.md`、`plan.md`、`tasks.md`、`research.md`、`data-model.md`、`quickstart.md` 及 checklist；专业术语（如 JWT、HMAC、IDOR、BOLA、MyBatis）可保留英文，代码标识符保留原文。
- Q: 请求体 merchantId 与 JWT/HMAC 上下文不一致时如何响应？ → A: **拒绝**：HTTP 403 + 业务码 `5101`，并写入安全审计（`cashier_security_audit`）；绝不静默忽略或改用请求体中的 merchantId。
- Q: 跨商户写操作（如对他人 paymentId 退款）对外响应？ → A: **统一 404**：HTTP 404 + 业务码 `5102`，对外 message 与「资源不存在」完全相同；审计记录内可记 `5103` 及真实原因，不得对外暴露。
- Q: Phase 0 是否包含管理后台审计查询？ → A: **全栈**：收银台侧建表并异步写入；`payflow-admin-server` 提供分页查询 API（`RISK`/`SUPER_ADMIN`）；`payflow-admin-client` 提供安全审计列表页（按商户/时间/outcome 筛选）。
- Q: 持久层拦截器是否覆盖 INSERT？ → A: **覆盖**：无有效商户上下文则拒绝 INSERT；有上下文则强制 `merchant_id` 与上下文一致（若 SQL 已带 `merchant_id` 则校验相等，未带则自动补全）。
- Q: Phase 0 改造的 API 覆盖范围？ → A: **全量商户 API**：`/api/v1/orders/**`、`/api/v1/payments/**`、`/api/v1/refunds/**`、`/api/v1/merchant/**`（HMAC）、`/api/v1/payment-links/**` 一次性纳入；公开端点（`/notify`、`/cashier`、`/public` 等）维持白名单不变。

## Constitution Compliance *(mandatory)*

在编写规范前，确认本功能涉及的宪法原则：

| 宪法原则 | 是否涉及 | 说明 |
|----------|----------|------|
| I. 模块边界纪律 | 是 | 隔离拦截器在 `payflow-cashier-server`；审计查询 API 在 `payflow-admin-server`；列表页在 `payflow-admin-client`；审计表在 `payflow_cashier` 库 |
| II. 支付渠道抽象 | 否 | 不涉及渠道改造 |
| III. 数据库分区 | 是 | 新增 `cashier_security_audit` 表（前缀 `cashier_`，属交易侧安全日志） |
| IV. API 响应规范 | 是 | 越权场景返回统一 `{ code, message, data }` 格式，错误码新增 5xxx 段商户安全相关码 |
| V. 密钥与配置安全 | 是 | 强化 JWT 上下文使用纪律，禁止前端覆盖；MerchantScope 加固 SQL 数据隔离 |
| 编码规范 | 是 | 新增类遵循命名/注释/成员顺序规范，构造器注入 |
| 数据库访问规范 | 是 | MyBatis 拦截器自动追加 `merchant_id = ?` 条件，禁止 SELECT * 不变 |
| 安全编码规范 | 是 | 本功能核心目标：修复已存在的 OWASP IDOR/BOLA 漏洞、纵深防御 |
| 测试规范 | 是 | 新增安全测试用例覆盖所有商户侧 API 越权场景，满足 80% 覆盖率 |

> 涉及的原则将在 `plan.md` Constitution Check 中逐项检查。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 商户无法跨越商户身份创建订单 (Priority: P1)

平台聚合接入了大量商户。商户 A 通过 JWT 登录后调用创建订单接口，当前代码（`OrderController.createOrder` 第 47-50 行）允许其在请求体中传入 `merchantId = "MerchantB"`，订单会落到商户 B 名下。商户 A 可借此污染商户 B 的交易数据、影响其手续费核算与对账。

**Why this priority**: 这是**当前已存在的可利用水平越权漏洞**，任何持有任一商户 Token 的攻击者均可伪造其他商户的订单。在聚合支付场景中，订单数据直接影响财务对账、商户排行、风控统计，是核心数据资产。

**Independent Test**: 商户 A 登录获取 JWT 后，向 `POST /api/v1/orders` 提交请求体 `{"merchantId":"MerchantB","amount":100,...}`，预期：接口拒绝并返回 HTTP 403、业务码 `5101`，写入安全审计；**绝不能**在商户 B 名下创建订单，也**不得**静默忽略请求体后改在商户 A 名下创建。

**Acceptance Scenarios**:

1. **Given** 商户 A 持有有效 JWT，**When** 调用 `POST /api/v1/orders` 且请求体未携带 `merchantId`，**Then** 订单创建到商户 A 名下
2. **Given** 商户 A 持有有效 JWT，**When** 调用 `POST /api/v1/orders` 且请求体携带 `merchantId = "MerchantB"`，**Then** 接口返回 403 并记录安全审计日志（含调用方 merchantId、试图冒用的 merchantId、请求路径、IP）
3. **Given** 商户 A 持有有效 JWT，**When** 调用 `POST /api/v1/orders` 且请求体携带 `merchantId = "MerchantA"`（与上下文一致），**Then** 订单正常创建（兼容当前部分前端显式传值的写法）

---

### User Story 2 - 商户无法跨越商户身份查询资源 (Priority: P1)

商户 A 通过 JWT 登录后，调用 `GET /api/v1/orders/{orderId}` 时，当前代码（`OrderController.getOrderDetail`）完全不校验该订单是否属于商户 A。只要 A 知道（或枚举到）商户 B 的某个 orderId，即可读取商户 B 的完整订单详情（金额、商品、回调地址、买家信息等）。同类问题适用于 `refunds/{refundId}`、`payments/{paymentId}` 等所有按资源 ID 查询的端点。

**Why this priority**: 这是**当前已存在的资源级越权漏洞**（OWASP BOLA），直接造成商户机密数据泄漏。orderId 等资源 ID 不应被假定为不可枚举的秘密。

**Independent Test**: 在系统中创建两条订单分别属于商户 A 与商户 B；以商户 A 的 JWT 调用 `GET /api/v1/orders/{B 的 orderId}`，预期返回 404（**不能区分"不存在"与"无权限"**，避免资源枚举），不返回 200。

**Acceptance Scenarios**:

1. **Given** 订单 `O-200001` 属于商户 B，**When** 商户 A 持 JWT 调用 `GET /api/v1/orders/O-200001`，**Then** 返回 404 且响应体不泄漏任何订单字段
2. **Given** 商户 A 持 JWT 调用 `GET /api/v1/orders/O-100001`（属于商户 A 自己），**Then** 正常返回订单详情
3. **Given** 商户 A 持 JWT 调用 `GET /api/v1/refunds/R-200001`（属于商户 B），**Then** 返回 404；调用 `GET /api/v1/payments/P-200001`（属于商户 B）同样返回 404
4. **Given** 商户 A 持 JWT 发起退款 `POST /api/v1/refunds` 指定 `paymentId = "P-200001"`（属于商户 B），**Then** 返回 HTTP 404 + 业务码 `5102`（对外 message 与资源不存在一致），写入安全审计（内部记 `5103`），**绝不能**真的对商户 B 的支付发起退款

---

### User Story 3 - 业务代码漏写 merchant_id 条件时仍不泄漏数据 (Priority: P1)

即使应用层做了所有权校验，仍可能出现业务代码遗漏（例如某个新增的列表接口忘记加 WHERE 条件）。系统必须在数据访问层提供**纵深防御**：MyBatis 拦截器自动给所有商户侧业务表的 SELECT/UPDATE/DELETE 语句追加 `merchant_id = ?` 条件，业务代码漏写也无法泄漏其他商户数据。

**Why this priority**: 单纯依赖业务代码自律不可持续——团队成员变更、新功能开发、紧急修复都可能引入遗漏。纵深防御使数据隔离不再依赖"开发者记性"，将安全保障从代码层下沉到框架层。

**Independent Test**: 临时构造一个故意未加商户过滤的查询方法（如直接 `selectList` 不带条件），用商户 A 的上下文调用，**预期返回的结果集中所有记录的 `merchant_id` 均等于商户 A**（拦截器自动注入了 WHERE 条件），而不是返回全表所有商户的数据。

**Acceptance Scenarios**:

1. **Given** 商户上下文为商户 A，**When** 任意 Mapper 方法对 `cashier_orders`、`cashier_payments`、`cashier_refunds`、`cashier_webhook_endpoint` 等商户业务表执行 SELECT，**Then** 即使 SQL 中未显式包含 `merchant_id` 条件，最终执行的 SQL 也会被拦截器追加该条件
2. **Given** 商户上下文为商户 A，**When** 任意 Mapper 方法对商户业务表执行 UPDATE/DELETE，**Then** 拦截器同样追加 `merchant_id = ?` 条件，商户 A 无法修改/删除商户 B 的数据
3. **Given** 商户上下文为商户 A，**When** 向商户业务表执行 INSERT 且未带 `merchant_id` 或携带 `merchant_id = "MerchantA"`，**Then** 写入成功且 `merchant_id` 为商户 A；**When** INSERT 携带 `merchant_id = "MerchantB"`，**Then** 拒绝执行（应用层或拦截器层，对外按写操作越权策略处理）
4. **Given** 当前线程为白名单场景（异步回调处理、对账/管理后台代理、定时任务等系统级访问），**When** 执行 Mapper 方法，**Then** 拦截器跳过自动注入（依赖该场景下的显式查询条件）
5. **Given** 拦截器命中并修改了 SQL，**When** 记录 DEBUG 日志，**Then** 日志中包含被拦截的 SQL 原文与注入后 SQL（便于排查）

---

### User Story 4 - 资源跨商户访问被发现时立即记录安全审计 (Priority: P2)

任何被识别为越权的请求（U1、U2 中的拒绝场景）必须被持久化记录到安全审计日志，供运营/安全团队事后排查潜在攻击。审计记录应包含足够信息以重建攻击过程，且不得包含敏感字段（卡号、密钥等）。

**Why this priority**: 越权请求往往是攻击侦察的征兆，单次拒绝不等于威胁消失。审计日志是事后取证、模式识别、风控规则调优的基础数据。

**Independent Test**: 触发一次越权请求（如 U2 场景 1），检查 `cashier_security_audit` 表中应新增一条记录，包含：发生时间、调用方 merchantId、请求方法、请求路径、试图访问的资源 ID、客户端 IP、User-Agent、判定结果（DENIED）、判定理由。

**Acceptance Scenarios**:

1. **Given** 商户 A 触发任意越权请求，**When** 拦截器拒绝该请求，**Then** `cashier_security_audit` 表新增一条 `outcome = DENIED` 的记录，包含完整上下文
2. **Given** 商户 A 触发越权请求 100 次（如脚本扫描），**When** 在 5 分钟内累计超过阈值（如 20 次），**Then** 触发告警（通过现有日志/指标通道，本 spec 不强制邮件/IM）
3. **Given** 审计记录写入，**When** 持有 `RISK` 或 `SUPER_ADMIN` 角色的运营人员在管理后台打开「安全审计」页面或调用 `GET /api/v1/admin/security/audit`，**Then** 可按 merchantId、时间范围、outcome、请求路径筛选并分页查看；无权限角色访问返回 403

---

### User Story 5 - 现有商户集成不被破坏 (Priority: P1)

系统已有商户在使用现有 API，部分商户的接入代码可能依赖"在请求体携带 merchantId"的既有行为。改造必须保证：
- 请求体携带的 `merchantId` **与 JWT 上下文一致**时，请求正常通过（兼容存量集成）
- 请求体携带的 `merchantId` 与 JWT 上下文**不一致**时才拒绝（修复越权）
- 请求体不携带 `merchantId` 时，自动从 JWT 上下文注入（推荐用法）

**Why this priority**: 任何安全修复都不能以破坏存量商户集成为代价，否则会被业务部门拒绝上线，最终导致漏洞无法关闭。

**Independent Test**: 取一份当前所有商户侧 API 的 Postman/集成测试集，执行三组：(a) 完全不传 merchantId，(b) 传与 JWT 一致的 merchantId，(c) 传与 JWT 不一致的 merchantId。前两组全部通过，第三组全部 403。

**Acceptance Scenarios**:

1. **Given** 商户 A 持 JWT/HMAC 调用 Phase 0 范围内全部商户 API（订单、支付、退款、Payment Link、商户查询等），**When** 请求体不携带 merchantId 或携带与认证上下文一致的 merchantId，**Then** 全部业务流程不受影响
2. **Given** 改造完成后，**When** 运行回归测试套件，**Then** 现有所有自动化测试（含端到端冒烟）100% 通过
3. **Given** 改造完成后，**When** 商户侧 API 文档（Swagger/OpenAPI）更新，**Then** 明确标注"请求体中的 merchantId 字段已弃用，将以 JWT 上下文为准；不一致将被拒绝"

---

### Edge Cases

- **HMAC 签名场景**（`/api/v1/merchant/**` 通过 `MerchantSignatureInterceptor` 鉴权）的越权防护需与 JWT 路径行为一致——签名验证通过得到的 merchantId 即上下文，任何资源访问必须按该 merchantId 校验所有权
- **后台代理调用商户数据**（如管理员代查某商户订单）：admin-server 调用 cashier 接口时使用 `X-Payflow-Internal-Token`，此场景下应豁免商户隔离拦截器（白名单），但仍需在审计中记录"系统调用"标记
- **Webhook 投递重试/对账批处理**：系统线程没有用户上下文，需通过明确的"系统模式"标记跳过拦截器；该标记必须由代码显式设置，禁止默认开启
- **资源不存在 vs 无权限**：必须返回相同的 404，不得通过响应差异让攻击者枚举资源是否存在
- **跨商户公共资源**（如渠道公共回调端点、PaymentLink 公开查询页）：不属于商户私有资源，须在白名单列出，不应被拦截器误判
- **JWT 中 merchantId 为空或异常值**：直接 401，不允许进入业务层凭"空"上下文操作数据
- **租户分流写入**：当前不涉及多租户分库；如未来引入，拦截器需扩展支持 tenant_id 维度
- **批量操作**：管理后台代调用进行批量数据操作时，必须显式提供 merchantId 列表，禁止"无条件"批量 UPDATE/DELETE

## Requirements *(mandatory)*

### Functional Requirements

**身份上下文与覆盖防护**：

- **FR-001**: 所有商户侧 API 的业务逻辑必须以认证上下文中的 `merchantId` 为唯一可信来源，Phase 0 **全量覆盖**：JWT 路径 `/api/v1/orders/**`、`/api/v1/refunds/**`、`/api/v1/payments/**`、`/api/v1/payment-links/**`；HMAC 路径 `/api/v1/merchant/**`（`/api/v1/auth/login|logout` 除外）。公开白名单路径不在此列
- **FR-002**: 当请求体/查询参数中显式携带 `merchantId` 字段时，系统必须与上下文 `merchantId` 比对：相等则放行；不等则**拒绝请求**（HTTP 403 + 业务码 `5101`）并写入安全审计，绝不允许以请求体值覆盖上下文，也**不得**静默忽略请求体后仅使用上下文继续执行业务
- **FR-003**: 当请求体未携带 `merchantId` 时，业务代码必须从上下文取值，禁止依赖前端传值

**资源所有权校验**：

- **FR-004**: 所有按资源 ID 操作（GET/PUT/DELETE 单条、以及以 paymentId/orderId/refundId 作为业务参数的写操作）的端点，必须在业务执行前验证该资源属于当前上下文 merchantId
- **FR-005**: 资源不存在与无权限访问（含跨商户读、跨商户写）必须返回**相同的 404 响应**（HTTP 404 + 业务码 `5102`，含相同 message），不得通过状态码、响应体、响应时间等任何方式让攻击者区分二者
- **FR-006**: 跨资源依赖（如发起退款引用 paymentId）必须级联校验——退款引用的 paymentId 必须属于当前商户，否则按 FR-005 返回统一 404（审计内记 `5103`）

**数据库层纵深防御**：

- **FR-007**: 系统必须实现持久层拦截器，在所有商户业务表（`cashier_orders`、`cashier_payments`、`cashier_refunds`、`cashier_webhook_endpoint`、`cashier_webhook_delivery`、`cashier_payment_link` 等）上：对 **SELECT/UPDATE/DELETE** 自动追加 `merchant_id = ?` 条件；对 **INSERT** 在无有效商户上下文时拒绝执行，有上下文时强制 `merchant_id` 与上下文一致（已带则校验相等，未带则自动补全）
- **FR-008**: 持久层拦截器必须提供明确的"系统模式"豁免机制（如 ThreadLocal 标志位 + try-finally 清理），仅供以下场景使用：
  - 异步回调解析（系统通过 channel_transaction_id 查 payment，此时尚未确立商户上下文）
  - admin-server 通过 internal token 代理调用
  - 对账/定时任务批处理
  - 单元测试中显式启用
- **FR-009**: 持久层拦截器豁免必须强制使用 try-finally 清理标志位，避免 ThreadLocal 污染线程池
- **FR-010**: 持久层拦截器拦截到 SQL 改写时，必须以 DEBUG 级别记录原始 SQL 与改写后 SQL，便于排查

**安全审计**：

- **FR-011**: 任何被识别为越权的请求（FR-002、FR-004 拒绝场景）必须持久化到安全审计表，字段至少包括：时间、调用方 merchantId、请求方法、请求路径、试图访问的资源 ID（如有）、客户端 IP、User-Agent、判定结果、判定理由
- **FR-012**: 安全审计表数据保留期最少 180 天，可通过定时任务归档至冷存储
- **FR-013**: 安全审计记录的查询能力必须全栈交付：`payflow-admin-server` 提供 `GET /api/v1/admin/security/audit` 分页接口，仅 `RISK` / `SUPER_ADMIN` 可访问；`payflow-admin-client` 提供「安全审计」列表页，支持 merchantId、时间范围、outcome、请求路径筛选
- **FR-014**: 越权请求的审计记录写入失败不得阻断主请求拒绝流程（异步写入 + 失败仅打 error 日志）；审计数据存于 `payflow_cashier` 库的 `cashier_security_audit` 表，由 admin-server 通过既有 cashier 数据源或内部接口只读查询（禁止跨库 JOIN）

**错误码与响应规范**：

- **FR-015**: 新增以下错误码（属 5xxx 商户段）：
  - `5101` "商户身份与请求不匹配"（FR-002）
  - `5102` "请求的资源不存在"（FR-005，含真实不存在和无权两种情况）
  - `5103` "操作的资源不属于当前商户"（写操作场景，用于审计但响应仍统一为 5102 文案以防泄漏）
- **FR-016**: 所有越权拒绝响应必须遵循统一 `{ code, message, data }` 格式：仅 **merchantId 字段与上下文不一致** 使用 HTTP 403 + `5101`；**资源 ID 越权或资源不存在**（含跨商户写）统一使用 HTTP 404 + `5102`

**白名单与系统通道**：

- **FR-017**: 必须维护明确的拦截器白名单路径清单，包括：所有渠道异步回调端点（`/api/v1/notify/**`）、PaymentLink 公开查询页（`/api/v1/public/**`）、收银台公开页（`/api/v1/cashier/**`）、健康检查、Swagger
- **FR-018**: admin-server 代理调用 cashier API 时必须使用现有 `X-Payflow-Internal-Token` 头识别，标记为"系统模式"绕过商户隔离拦截器，但仍需在 access 日志记录 actor

**改造范围与回归保证**：

- **FR-019**: 改造必须包含针对 Phase 0 全量商户 API（`OrderController`、`PaymentController`、`RefundController`、`MerchantQueryController`、`PaymentLinkController`）的安全测试套件，覆盖 U1、U2、U5 的所有验收场景
- **FR-020**: 改造必须在 CI 流水线中加入静态检查规则：商户侧 Controller 方法签名若包含 `@PathVariable` 形参且变量名以 `Id` 结尾（如 `orderId`、`refundId`、`paymentId`），方法体必须调用 `MerchantContext` 的所有权校验工具（或被 `MerchantResourceOwnershipInterceptor` 拦截器配置覆盖），否则 CI 失败

### Key Entities *(include if feature involves data)*

- **MerchantContext**: 商户认证上下文。承载经过认证后的可信信息——`merchantId`、`authMode`（JWT/HMAC/INTERNAL）、`requestPath`、`clientIp`。生命周期与请求一致，通过拦截器写入，业务代码只读。不允许业务代码反向修改。
- **MerchantScopeInterceptor**: MyBatis 持久层拦截器。识别商户业务表的 SQL 语句，自动追加 `merchant_id = ?` 条件。提供"系统模式"豁免标志位，仅供受控场景使用。
- **MerchantResourceOwnershipInterceptor**: Web 层资源所有权拦截器。根据路径变量约定（`orderId`/`refundId`/`paymentId`）自动反查资源所属 merchantId，与上下文比对，不匹配则拒绝。
- **SecurityAudit**: 安全审计记录实体。归属 `cashier_security_audit` 表。表结构遵循只增不改原则，索引按 merchantId + 时间组合。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 在改造完成后的渗透测试中，**0 笔**水平越权请求能够成功访问/修改其他商户的数据（覆盖 U1、U2 所有验收场景，自动化用例 ≥ 30 个）
- **SC-002**: 所有商户侧 API 端点（按 Controller 方法计 ≥ 25 个端点）**100%** 接入认证上下文优先与所有权校验
- **SC-003**: 持久层拦截器对商户业务表（≥ 6 张表）的 SELECT/UPDATE/DELETE 拦截**100%** 注入 `merchant_id` 条件；INSERT **100%** 强制或校验 `merchant_id` 与上下文一致（通过 SQL 日志统计验证，运行 24 小时全量样本不少于 10000 条）
- **SC-004**: 持久层拦截器"系统模式"豁免的使用位置必须在代码中明确标注，全项目豁免点 ≤ 10 处，每处需在 PR 中说明理由
- **SC-005**: 改造后回归测试套件（现有 39 个 + 新增 ≥ 30 个安全用例）**100%** 通过
- **SC-006**: 越权请求被识别后审计写入成功率 ≥ 99.9%（异步写入失败仅记日志，不影响业务拒绝）
- **SC-007**: 资源不存在与无权限访问的响应**完全不可区分**（HTTP 码、响应体、平均响应时间差异 < 5ms）
- **SC-008**: 改造完成后 30 天内，安全审计表中的 `DENIED` 记录可被运营人员通过管理后台正常查询，过滤性能 < 1 秒（百万级数据规模）
- **SC-009**: 现有商户已上线集成的回归冒烟测试**100%** 通过——不引入任何破坏性变更
- **SC-010**: CI 静态检查规则（FR-020）正式启用后，主分支不再合入未做所有权校验的新端点

## Assumptions

- 当前管理后台（admin-server）拥有更完善的鉴权与数据范围控制（通过 `AdminRequestContext.merchantScope`），本 spec **不修复** admin 侧既有越权问题，但 **包含** 安全审计只读查询 API 与管理端列表页（见 FR-013）
- 商户子账号体系（一商户多用户、内部 RBAC）属于后续 Phase 3a 的独立 spec，本 spec 仅处理"商户即用户"的扁平模型下的隔离问题
- 假设系统已具备 JWT 拦截器（`JwtAuthInterceptor`）和 HMAC 签名拦截器（`MerchantSignatureInterceptor`）作为认证入口，本 spec 在此基础上增加授权与隔离层
- 假设 MyBatis-Plus 拦截器机制（`InnerInterceptor`/`Interceptor`）能可靠地拦截 SQL 改写，并在所有 Mapper 调用路径生效——基于 MyBatis-Plus 3.5.7 的官方能力
- 假设管理后台代理调用通过现有的 `X-Payflow-Internal-Token` 机制识别系统调用方，无需引入新认证机制
- 假设审计表写入采用本地 MySQL，未来若数据量过大可引入归档机制（不在本 spec 范围）
- 假设 admin-server 现有的 `RISK`/`SUPER_ADMIN` 角色机制可直接复用于安全审计查询端点的授权
- 当前 `MerchantSignatureInterceptor` 实现已正确将 merchantId 注入请求属性，本 spec 不重新设计签名机制
- 改造期间不涉及表结构兼容性中断；新增 `cashier_security_audit` 表通过 Flyway 增量迁移
