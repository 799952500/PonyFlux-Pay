# Feature Specification: 产品质量优化与升级专项

**Feature Branch**: `014-product-optimization`
**Created**: 2026-05-29
**Status**: Draft
**Input**: User description: "本次旨在发现项目的优化点和升级点，不横向扩展功能，只为把产品功能做的更好，请针对项目的情况提出意见和规划"

## Constitution Compliance *(mandatory)*

在编写规范前，确认本功能涉及的宪法原则：

| 宪法原则 | 是否涉及 | 说明 |
|----------|----------|------|
| I. 模块边界纪律 | [x] 是 | 涉及 cashier/admin/recon/channels 多模块；统一 `JwtUtils`、`R<T>` 拟下沉到 `payflow-common` |
| II. 支付渠道抽象 | [x] 是 | 微信回调验签补齐；查单/超时关单改走 `PayChannelPaymentOpenService`，消除硬编码 Handler 依赖 |
| III. 数据库分区 | [x] 是 | 不新增业务表；批量写入、索引利用、`bill_date` 冗余列等优化按 admin_/cashier_/recon_ 前缀归属 |
| IV. API 响应规范 | [x] 是 | 统一三套响应包装（admin `ApiResponse` / cashier `R` / recon `R` / 手工 `Map`）为单一 `{code,message,data}` |
| V. 密钥与配置安全 | [x] 是 | 收银台 `channelConfig` 明文加密；dev 默认密钥与日志脱敏；`.env.example` 与 prod 校验 |
| 编码规范 | [x] 是 | 构造器注入统一（消除 setter/字段注入）、包装类型、命名规范 |
| 数据库访问规范 | [x] 是 | 分页 maxLimit、批量 insert/update、避免 `DATE()` 致索引失效、N+1 修复 |
| 安全编码规范 | [x] 是 | 回调验签与防重放、支付幂等/乐观锁、日志脱敏、登出黑名单健壮性 |
| 测试规范 | [x] 是 | 核心支付/对账补最小测试、E2E 纳入 CI、JaCoCo 门禁落地 |

> 涉及的原则将在 `plan.md` Constitution Check 中逐项检查。

## User Scenarios & Testing *(mandatory)*

> 本专项不新增对外业务功能，所有 User Story 均为「把现有能力做得更可靠、更快、更一致、更易维护」。每个 Story 都是可独立交付、独立验证的质量切片。

### User Story 1 - 支付资金安全与回调正确性加固 (Priority: P1) 🎯 MVP

作为支付平台的安全与资金负责人，我需要确保所有支付渠道的异步回调都经过严格验签与防重放，回调与同步返回不会因并发或重复而导致订单状态错乱或商户重复收到通知，从而保证资金记账的正确性与不可抵赖性。

**Why this priority**: 这是支付网关的生命线。当前微信 v3 回调仅做 AES 解密、**完全没有 RSA 平台证书验签**（支付宝/银联已实现），伪造或重放回调可能触发 `handlePaymentSuccess`；同步下单 `paidImmediately` 与异步回调路径可能叠加，导致商户重复收到 `PAYMENT_SUCCESS`；`Payment` 无乐观锁/条件更新，双回调存在竞态。任何一项被利用都可能造成直接资金损失或商誉损失，因此优先级最高。

**Independent Test**: 向 `/notify/wechat` 发送一个篡改过签名的回调，验证系统拒绝处理且订单状态不变；连续发送两次合法回调，验证订单只被确认一次、商户只收到一次成功通知。

**Acceptance Scenarios**:

1. **Given** 一笔微信 Native 支付处于 PROCESSING，**When** 收到带有伪造/缺失签名的回调，**Then** 系统拒绝处理（返回失败）、记录告警，订单状态保持不变。
2. **Given** 同一笔支付的合法回调被重复投递两次，**When** 第二次回调到达，**Then** 系统识别为重复事件，订单仅确认一次，商户 Webhook 仅触发一次。
3. **Given** 一笔支付走同步下单即时成功（`paidImmediately`）路径，**When** 渠道随后又发来异步回调，**Then** 不产生第二次商户通知（按 `paymentId + status` 幂等）。
4. **Given** 收银台数据库中的渠道账户配置，**When** 直接查询 `payflow_cashier` 的 `channelConfig` 字段，**Then** 渠道密钥为密文存储而非明文 JSON。
5. **Given** 两个并发线程同时处理同一笔回调，**When** 更新订单/支付状态，**Then** 仅有一个成功（条件更新/乐观锁），另一个无副作用。

---

### User Story 2 - 高并发下单与对账吞吐性能优化 (Priority: P1)

作为运维与商户，我需要系统在高并发下单和大账单对账时保持稳定的响应时间，不因渠道慢调用拖垮数据库连接池，不因逐条写库让对账任务越跑越慢。

**Why this priority**: 当前下单整个流程（落库 + 调渠道 HTTP）在同一个 `@Transactional` 内，渠道慢/超时会长时间占用 DB 连接，高并发下连接池可能被打满，下单 P99 被渠道 RTT 绑架；对账解析、比对、差异标注均为逐条 INSERT/UPDATE，万级账单线性劣化；多个查询存在 N+1 与"假分页"（全量加载后内存裁剪），分页接口无 `pageSize` 上限存在 DoS 风险。这些直接影响系统在真实负载下的可用性。

**Independent Test**: 用并发压测脚本模拟渠道响应延迟 2s 的 200 并发下单，验证 DB 连接池不被耗尽、下单本地落库成功率不受渠道 RTT 影响；用 5 万条账单跑对账，对比优化前后总耗时显著下降。

**Acceptance Scenarios**:

1. **Given** 渠道接口响应缓慢，**When** 高并发发起下单，**Then** 本地订单/支付落库在短事务内完成，调渠道在事务外执行，DB 连接不被长事务占用。
2. **Given** 一份包含数万条记录的渠道账单，**When** 执行解析与比对，**Then** 账单记录与差异记录以批量方式写入（按批 500~1000），总耗时相比逐条写入显著降低。
3. **Given** 任意分页查询接口（支付账号、商户、流失预警等），**When** 客户端传入超大 `pageSize`，**Then** 服务端将其裁剪到上限（≤100），不会全量加载或拖垮数据库。
4. **Given** 列表/详情查询（回调列表、渠道账户、长尾统计、路由），**When** 加载数据，**Then** 不出现逐条二次查询（N+1），关联数据通过批量查询或 JOIN 一次取出。
5. **Given** 对账与报表按账单日聚合查询，**When** 数据量增大，**Then** 时间条件使用范围查询而非对列套 `DATE()/COALESCE()`，可命中索引。

---

### User Story 3 - 关键操作路径不再静默失败 (Priority: P1)

作为管理后台运营人员与收银台付款用户，当任何关键操作或数据加载失败时，我必须能看到明确的错误提示与重试入口，而不是面对空白页面、卡死的加载态或"看起来正常实则没数据"的假象。

**Why this priority**: 静默失败直接违背"无静默失败"的既有产品目标（SC-006），且发生在最关键的路径上：对账工单详情页加载与认领/指派/完成操作无 `try/catch` 与 loading；通知中心、报告详情、SLA 保存失败被静默吞掉；收银台在订单不存在/网络失败时主区域空白、"我已支付"确认在异常分支永久卡 loading。这些会让运营无法处置差异、让付款用户陷入困惑，损害产品可信度。

**Independent Test**: 关闭后端服务后访问对账工单详情页与收银台付款页，验证两者都显示明确错误卡片与重试入口而非白屏/永久 loading；触发"我已支付"在无支付结果时，验证按钮 loading 正确复位。

**Acceptance Scenarios**:

1. **Given** 后端不可用，**When** 打开对账工单详情页，**Then** 页面显示错误状态与重试，而非空白；认领/指派/完成按钮在请求中显示 loading、失败时弹出错误提示。
2. **Given** 通知/报告详情/SLA 保存等请求失败，**When** 操作触发，**Then** 用户看到 `ElMessage` 错误提示，不再静默吞掉。
3. **Given** 订单不存在或网络失败，**When** 进入收银台付款页（PC/H5），**Then** 主区域显示错误卡片与重试，而非空白。
4. **Given** 二维码支付场景缺少支付结果，**When** 用户点击"我已支付"，**Then** 确认按钮的 loading 状态正确复位，不会永久卡死。
5. **Given** 列表类页面无数据，**When** 加载完成，**Then** 显示统一的空状态（`el-empty`）而非仅空表头，使"无结果"与"加载失败"可区分。

---

### User Story 4 - 系统一致性与可维护性收敛 (Priority: P2)

作为开发与维护人员，我需要消除项目中的重复实现与多套并存的约定，使相同的事情只有一种做法，降低维护成本与行为不一致风险。

**Why this priority**: 当前存在两份 `JwtUtils`（静态工具类 vs Spring Bean，claims 结构不同）、三套响应包装（admin `ApiResponse` / cashier `R` / recon `R`，外加 Controller 手工 `Map`）、查单与超时关单路径硬编码具体 Handler（绕过策略模式，新增渠道要改多处）。这些不一致是 bug 温床，也违反模块边界与渠道抽象原则。优先级 P2，因其影响可维护性而非即时可用性。

**Independent Test**: 检查代码库中 `JwtUtils` 只剩一份（位于 `payflow-common`）、响应包装统一为单一 `R<T>`；为支付宝渠道触发主动查单，验证其经由统一的 `PayChannelPaymentOpenService` 完成而非硬编码微信 Handler。

**Acceptance Scenarios**:

1. **Given** 项目需要生成/校验 JWT，**When** 查找实现，**Then** 仅存在一处统一实现（下沉到 `payflow-common`），admin 与 cashier 复用且行为一致。
2. **Given** 任意 Controller 返回结果，**When** 前端接收，**Then** 结构统一为 `{code, message, data}`，不再有三套包装并存。
3. **Given** 支付宝/银联订单，**When** 系统主动查单或超时核对，**Then** 通过策略定位器调用对应渠道服务，不再仅支持微信、不再硬编码 Handler。
4. **Given** 超时关单流程，**When** 决定是否关单，**Then** 先经渠道查单确认未支付（查单逻辑真实实现，不再恒返回 false）才关单。

---

### User Story 5 - 可观测性与运维就绪 (Priority: P2)

作为运维人员，我需要三个后端服务都能被监控探活、关键指标可采集、生产日志不泄露敏感信息，并且一键拉起环境（docker-compose）真的能成功，从而具备稳定运行与快速排障的基础。

**Why this priority**: 当前 recon-server 未暴露 metrics/prometheus，对账任务耗时/失败率/差异量无法监控；cashier/recon 默认日志级别为 DEBUG（含 SQL 参数、商户号、金额），渠道错误响应全文打日志，存在敏感信息泄露；`docker-compose.yml` 初始化引用了**已删除**的 `sql/full-reseed-payflow-demo.sql`，首次 `docker compose up` 必然失败；`install_demo_db.py` 的 Flyway 历史仅同步到 V8/V5，与实际 V9–V11 不一致，demo 库启动后可能冲突。运维就绪是产品可上线的前提。

**Independent Test**: 在干净环境执行 `docker compose up`，验证数据库初始化成功、三服务健康检查通过；访问 recon-server 的 metrics 端点验证可采集对账指标；检查 prod 日志无明文密钥/未脱敏敏感字段。

**Acceptance Scenarios**:

1. **Given** 干净环境，**When** 执行 `docker compose up`，**Then** 数据库初始化成功（不再引用不存在的 SQL 文件），三服务可正常启动并通过健康检查。
2. **Given** demo 数据库由 `install_demo_db.py` 安装，**When** 启动 admin/cashier，**Then** Flyway 历史与实际迁移版本一致（含 V9–V11），不产生重复执行或冲突。
3. **Given** recon-server 运行中，**When** 采集监控指标，**Then** 可获得对账任务耗时、失败率、差异数量等指标。
4. **Given** 生产环境运行，**When** 检查日志输出，**Then** 默认日志级别为 INFO，无明文密钥、无未脱敏的敏感字段（商户号、金额、完整渠道响应体）。
5. **Given** 容器编排环境，**When** 探活三个 Java 服务，**Then** 各服务定义了应用级 healthcheck 并能正确反映就绪状态。

---

### User Story 6 - 国际化（i18n）真正落地 (Priority: P2)

作为面向多语言用户的产品，管理后台与收银台的界面文本应能真正随语言切换显示，而不是"配置了 i18n 框架却几乎没有页面使用"。

**Why this priority**: admin-client 已配置 i18n，但全项目仅 5 个文件使用 `useI18n`，业务页面（onboarding、用户、角色、订单等）大量硬编码中文，`en-US.ts` 形同虚设；cashier-client 安装了 `vue-i18n` 却未在入口注册、收银台页面全硬编码。这使既有的语言切换能力名存实亡。优先级 P2，因其影响国际化体验而非核心交易正确性。

**Independent Test**: 将 admin-client 切换为 en-US，验证主要 CRUD 页面（订单、商户、渠道、用户）表头/按钮/状态显示英文；cashier-client 注册 i18n 后，收银台主页面文本走 `$t`。

**Acceptance Scenarios**:

1. **Given** admin-client 语言切换为 en-US，**When** 浏览主要业务页面，**Then** ≥80% 的 UI 字符串（表头、按钮、筛选、对话框）显示对应语言。
2. **Given** cashier-client，**When** 应用启动，**Then** i18n 已在入口注册，收银台/注册/收据页文本通过 `$t` 渲染。
3. **Given** 通知相对时间显示，**When** 切换语言，**Then** 时间本地化跟随当前 locale，而非写死中文。

---

### User Story 7 - 核心质量门禁与测试加固 (Priority: P3)

作为工程团队，我需要核心支付与对账链路具备最小自动化测试、E2E 纳入 CI、覆盖率门禁可执行，使后续优化与重构有回归保护网，而不是依赖人工验证。

**Why this priority**: 当前核心支付链路（`PaymentServiceImpl`、回调、退款）与对账引擎（`ReconCompareService`、`BillParser`）几乎无测试；Playwright E2E 存在但未进 CI；项目宪法要求 JaCoCo ≥80% 但根 POM 未配置；部分命名带 `IT` 的实为 Mock 单测、`DashboardMetricsMapperTest` 在无 DB 时静默空跑。这些导致"CI 通过"不等于质量可信。优先级 P3，作为前述优化的长期保障。

**Independent Test**: 运行 `mvn test`，验证新增的支付状态机与对账比对（四类 diff）测试通过；在 CI 中触发 E2E job，验证对账与收银台关键流程被自动验证；查看 JaCoCo 报告生成。

**Acceptance Scenarios**:

1. **Given** 核心支付状态机，**When** 运行测试，**Then** 覆盖下单→路由→回调成功/失败/幂等的关键路径（Mock 渠道）。
2. **Given** 对账比对逻辑，**When** 用 fixture CSV 运行测试，**Then** 正确产出 `CHANNEL_ONLY/LOCAL_ONLY/AMOUNT_MISMATCH/STATUS_MISMATCH` 四类差异。
3. **Given** CI 工作流，**When** 提交触发，**Then** 执行 Playwright E2E（或 nightly），对账与收银台关键流程被验证。
4. **Given** 构建流程，**When** 执行测试阶段，**Then** 生成 JaCoCo 覆盖率报告（初期阈值可设较低并逐步提高），`DashboardMetricsMapperTest` 不再静默跳过。

---

### User Story 8 - 文档与配置一致性修正 (Priority: P3)

作为新加入的开发者或对接方，我需要项目文档准确反映当前实现，使我能按文档正确联调与部署，而不是被过期描述误导。

**Why this priority**: `docs/reconciliation.md` 仍描述 admin→recon 的 HTTP 代理与已删除的 `AdminReconClient`/`full-reseed`，而实际 recon-server 已无 Controller、admin 直读 DB；`CLAUDE.md` 对账架构描述偏旧；`sql/README.md` 与 `docker-compose.yml` 对初始化入口的描述矛盾；缺少 `.env.example`。文档失真会持续制造联调与部署成本。优先级 P3。

**Independent Test**: 按更新后的 `docs/reconciliation.md` 与 README 完成一次环境搭建，验证步骤可执行、无指向已删除文件的死链。

**Acceptance Scenarios**:

1. **Given** `docs/reconciliation.md`，**When** 阅读对账架构，**Then** 描述与实现一致（admin 直读 DB、recon-server 仅批处理、初始化用 `install_demo_db.py`）。
2. **Given** `CLAUDE.md` 与 `sql/README.md`，**When** 查阅对账与初始化说明，**Then** 与 `docker-compose.yml` 不再矛盾，权威初始化入口统一。
3. **Given** 需要本地或容器部署，**When** 查找环境变量清单，**Then** 存在 `.env.example`（不含真实密钥）说明所有必填项。

---

### Edge Cases

- 微信回调验签上线后，存量未配置平台证书的渠道账户如何处理？→ 缺证书时回调按失败处理并告警，不静默放行。
- 下单事务拆分后，"本地已落库但调渠道失败"如何对账？→ 订单标记为下单失败/待重试，依赖既有超时扫描与查单兜底，不产生悬挂中间态。
- 批量写入对账差异时单批部分失败如何处理？→ 整批回滚并记录失败批次，避免差异记录部分写入造成对账结论错误。
- 分页 `pageSize` 被裁剪到上限后，客户端如何感知？→ 响应返回实际生效的分页参数，前端按返回值渲染。
- i18n 抽取过程中漏翻的字符串如何兜底？→ 缺失 key 回退显示中文默认值，不出现空白或 key 名。
- 统一响应结构变更是否破坏前端契约？→ 前端 Axios 解包逻辑已统一，变更需与 `CONTRACT_MATRIX.md` 同步并回归验证。
- 日志级别下调为 INFO 后，排障所需的关键链路信息是否仍可见？→ 关键状态变更保留 INFO，详细 DEBUG 仅 dev profile 开启。

## Requirements *(mandatory)*

### Functional Requirements

**支付资金安全与回调正确性（US1）**

- **FR-001**: 系统 MUST 对微信 v3 异步回调执行平台证书 RSA 验签与防重放（时间戳窗口）校验，验签失败拒绝处理并告警，使其与支付宝/银联渠道的验签能力对齐。
- **FR-002**: 系统 MUST 保证支付回调处理的幂等性——重复回调、以及"同步即时成功 + 异步回调"叠加场景下，订单只确认一次、商户 Webhook/通知只触发一次（按 `paymentId + status` 或事件去重）。
- **FR-003**: 系统 MUST 对支付成功状态更新使用条件更新或乐观锁，确保并发回调下仅一次生效、无重复副作用。
- **FR-004**: 收银台 `payflow_cashier` 中渠道账户的 `channelConfig`（含渠道密钥）MUST 加密存储，与 admin 侧加密策略一致，数据库泄露不致明文暴露密钥。

**性能与吞吐（US2）**

- **FR-005**: 下单流程 MUST 将本地落库与调用渠道 HTTP 解耦，避免在单个事务内同步等待渠道响应，防止 DB 连接被长事务占用。
- **FR-006**: 对账解析、比对与差异标注 MUST 采用批量写入/更新（合理批大小），避免逐条 INSERT/UPDATE 造成的线性劣化。
- **FR-007**: 所有分页查询接口 MUST 强制 `pageSize` 上限（≤100），并消除"全量加载后内存裁剪"的假分页。
- **FR-008**: 已识别的 N+1 查询（回调列表、渠道账户 DTO、长尾统计、路由同步、支付宝回调验签账户匹配等）MUST 改为批量查询或 JOIN 一次取出。
- **FR-009**: 对账与报表按日期聚合的查询 MUST 避免对时间列套用 `DATE()/COALESCE()` 导致索引失效，改用范围查询或冗余可索引日期列。

**可靠的前端体验（US3）**

- **FR-010**: 关键页面（对账工单详情、报告详情、收银台付款页）MUST 具备 loading 态、错误态与重试入口，加载/操作失败时显示明确提示，杜绝静默失败与白屏。
- **FR-011**: 列表类页面 MUST 提供统一空状态（`el-empty`），使"无结果"与"加载失败"可区分。
- **FR-012**: 收银台"我已支付"确认在异常/缺少支付结果分支 MUST 正确复位 loading，不得永久卡死。

**一致性与可维护性（US4）**

- **FR-013**: 系统 MUST 将 `JwtUtils` 收敛为单一实现（下沉 `payflow-common`），admin 与 cashier 复用，claims/行为一致。
- **FR-014**: 系统 MUST 将响应包装统一为单一 `{code, message, data}` 结构，消除 admin `ApiResponse`、cashier/recon 各自 `R`、Controller 手工 `Map` 并存。
- **FR-015**: 主动查单与超时关单 MUST 经由 `PayChannelPaymentOpenService` 策略定位调用，支持微信/支付宝/银联，消除硬编码 Handler；超时关单前 MUST 真实查单确认未支付。

**可观测性与运维就绪（US5）**

- **FR-016**: recon-server MUST 暴露与 admin/cashier 对齐的监控指标端点，并提供对账任务耗时、失败率、差异数量等业务指标。
- **FR-017**: 三服务生产环境默认日志级别 MUST 为 INFO，且日志 MUST 不输出明文密钥与未脱敏敏感字段（商户号、金额、完整渠道响应体）。
- **FR-018**: `docker-compose.yml` 的数据库初始化 MUST 指向有效的初始化入口（不再引用已删除的 `sql/full-reseed-payflow-demo.sql`），保证干净环境一键拉起成功。
- **FR-019**: `install_demo_db.py` 的 Flyway 历史 MUST 与实际迁移版本（含 V9–V11）同步，避免 demo 库与 Flyway 冲突。
- **FR-020**: 三个 Java 服务 MUST 在容器编排中定义应用级 healthcheck。

**国际化（US6）**

- **FR-021**: admin-client 主要业务页面 MUST 将硬编码中文抽取到 i18n，使 ≥80% UI 字符串支持中英文切换；缺失 key MUST 回退默认中文。
- **FR-022**: cashier-client MUST 在入口注册 vue-i18n，收银台/注册/收据主页面文本通过 i18n 渲染；相对时间本地化跟随当前 locale。

**质量门禁（US7）**

- **FR-023**: 核心支付链路（下单→路由→回调）与对账引擎（比对四类 diff、账单解析）MUST 具备最小自动化测试覆盖。
- **FR-024**: CI MUST 纳入 Playwright E2E（或 nightly），覆盖对账与收银台关键流程；现有静默跳过的测试（如 `DashboardMetricsMapperTest`）MUST 改为显式条件跳过而非假绿。
- **FR-025**: 构建 MUST 生成 JaCoCo 覆盖率报告（初期阈值可低并逐步提高）。

**文档一致性（US8）**

- **FR-026**: `docs/reconciliation.md`、`CLAUDE.md`、`sql/README.md` MUST 更新为与当前实现一致（recon 无 Controller、admin 直读 DB、初始化入口统一），并消除指向已删除文件的死链。
- **FR-027**: 项目 MUST 提供 `.env.example`（不含真实密钥），列出 docker-compose / prod 所需的全部环境变量。

### Key Entities *(include if feature involves data)*

> 本专项以优化既有实现为主，不新增业务表。仅涉及以下数据访问/结构层面的调整对象：

- **加密渠道配置（Encrypted Channel Config）**: 收银台 `PayChannelAccount.channelConfig` 由明文 JSON 改为密文存储，读取时解密；与 admin `PaymentAccount` 加密策略一致。
- **统一响应体（Unified Response `R<T>`）**: 跨 admin/cashier/recon 的单一响应封装 `{code, message, data}`，替代当前三套并存结构。
- **统一 JWT 工具（Unified JwtUtils）**: 下沉到 `payflow-common` 的单一 JWT 生成/校验组件，统一 claims 结构。
- **支付幂等标识（Payment Idempotency Key）**: 以 `paymentId + status` 或事件 ID 作为回调/通知去重依据。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 伪造或重放的微信回调 100% 被拒绝，合法回调重复投递时订单仅确认一次、商户通知仅一次。
- **SC-002**: 收银台数据库中渠道密钥 100% 以密文存储，明文检索无法获取密钥。
- **SC-003**: 在渠道响应延迟 2s 的 200 并发下单压测中，DB 连接池不被耗尽，本地下单落库成功率 ≥99%。
- **SC-004**: 5 万条账单的对账总耗时相比逐条写入下降 ≥60%。
- **SC-005**: 所有分页接口在任意 `pageSize` 入参下单次返回行数 ≤100。
- **SC-006**: 关键页面（对账工单详情、收银台付款页）在后端不可用时 100% 显示错误提示与重试入口，无白屏/永久 loading；列表页 100% 具备可区分的空状态。
- **SC-007**: 代码库中 `JwtUtils` 与响应包装各收敛为单一实现；支付宝/银联可经统一入口主动查单。
- **SC-008**: 干净环境 `docker compose up` 一次性成功初始化并启动三服务（健康检查通过）。
- **SC-009**: 生产日志中无明文密钥与未脱敏敏感字段；recon-server 监控指标可被采集。
- **SC-010**: admin-client 主要页面 ≥80% UI 字符串支持中英文切换；cashier-client 收银台主页面文本走 i18n。
- **SC-011**: 核心支付与对账测试纳入 `mvn test` 并通过；CI 输出 JaCoCo 覆盖率报告且执行 E2E（或 nightly）。

## Assumptions

- **范围边界**：本专项聚焦"把现有功能做得更好"，**不新增任何对外业务功能**；所有改动均为安全加固、性能优化、可靠性提升、一致性收敛、可观测性与文档质量。
- **优先级取舍**：以 P1（支付安全/正确性、性能、关键体验）为 MVP 必交付；P2（一致性、运维就绪、i18n）为次轮；P3（测试门禁、文档）为长期保障，可分迭代落地。纯抛光项（虚拟滚动、防抖、ECharts composable、Drawer 抽象、bundle 分包、移动端适配等）记录在册但默认排在 P3 之后，不在本专项强制范围内。
- 微信 APIv3 平台证书/密钥沿用现有渠道账户配置（`channelConfig`）获取，不引入额外密钥管理系统。
- 收银台密钥加密复用 `payflow-common` 的 `AesEncryptor` 与既有 master key 注入方式。
- 性能优化以现有 MySQL + Redis 技术栈为前提，不引入新的中间件。
- i18n 实际覆盖范围：admin-client 优先主要 CRUD 与对账页面，cashier-client 覆盖收银台主流程页面；不要求 100% 全量翻译。
- 测试加固以核心交易与对账路径为优先，JaCoCo 初期阈值从较低水平起步逐步提高，不要求一次性达到宪法的 80%。
- 已在前轮（specs/001、004）完成并验证的项（CORS 白名单、admin 异常脱敏、退款策略模式、admin 侧密钥加密、`merchant_order_no` 索引、路由懒加载、统一 Axios、ECharts 按需引入等）不在本专项重复处理。
