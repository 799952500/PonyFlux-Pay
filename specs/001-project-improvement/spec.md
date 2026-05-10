# Feature Specification: 项目健壮性改进计划

**Feature Branch**: `001-project-improvement`
**Created**: 2026-05-10
**Status**: Draft
**Input**: 检查我的项目，你认为还有哪些功能需要完善，或者有些功能很粗糙的，请列出来，并推荐一个改进计划

## User Scenarios & Testing

### User Story 1 - 微信支付回调能正常处理 (Priority: P1)

作为商户，当我发起微信支付后，支付结果回调必须能被系统正确处理，从而订单状态能自动更新为"已支付"。

**Why this priority**: `WxPayNotifyHelper.getWxPayApiV3Key()` 直接抛出 `UnsupportedOperationException`，导致所有微信支付回调都崩溃。这意味着微信支付成功后，系统永远无法自动确认支付状态，这是线上阻断性 bug。

**Independent Test**: 在本地启动 cashier-server，用 Postman 向 `/notify/wechat` 发送模拟微信回调请求，验证订单状态从"支付中"变为"已支付"，付款记录更新为"成功"。

**Acceptance Scenarios**:

1. **Given** 一笔微信 Native 支付已发起（订单状态=PAYING），**When** 微信回调到达 `/notify/wechat` 且验签通过，**Then** 系统解析通知成功，订单状态更新为 PAID，付款记录状态为 SUCCESS。
2. **Given** 一笔支付已发起，**When** 微信回调到达但 APIv3 密钥获取失败，**Then** 系统记录错误日志，返回 500，订单状态保持不变。
3. **Given** APIv3 密钥已配置在渠道账户 JSON 中，**When** 系统需要解密回调资源，**Then** 从 `ChannelConfigHolder` 提取密钥，不再抛出 `UnsupportedOperationException`。

---

### User Story 2 - 异常信息不泄露到前端 (Priority: P1)

作为系统安全管理员，我需要确保运行时异常的内部错误信息不会通过 API 响应泄露给前端用户，防止攻击者获取系统内部路径、SQL 或堆栈信息。

**Why this priority**: cashier-server 和 recon-server 的 `GlobalExceptionHandler` 中的 `handleRuntimeException` 直接将 `e.getMessage()` 返回给前端，这符合 OWASP 漏洞分类。20+ 处 `BizException` 包装了底层异常的 message。

**Independent Test**: 在 cashier-server 中模拟一个 RuntimeException（如 NPE），调用任意 API，验证响应中的 `message` 字段为"服务器内部错误"而非 NPE 详情。

**Acceptance Scenarios**:

1. **Given** cashier-server 运行中，**When** 任意未捕获的 RuntimeException 发生，**Then** API 返回 `{"code":500,"message":"服务器内部错误"}`，不包含异常堆栈或内部路径。
2. **Given** admin-server 运行中，**When** RuntimeException 发生，**Then** 行为与现有实现一致（返回通用错误信息，不泄露细节）。
3. **Given** cashier-server 运行中，**When** 底层 HTTP 调用失败（如连接超时），**Then** API 返回业务错误码和通用描述，不暴露原始异常消息。

---

### User Story 3 - 密码安全存储 (Priority: P1)

作为系统安全管理员，商户登录密码必须使用安全的哈希算法（bcrypt/argon2），而非 MD5。

**Why this priority**: cashier-server `AuthServiceImpl` 使用 MD5 做密码哈希，MD5 已被证明不安全且极易被彩虹表破解。admin-server 已正确使用 BCryptPasswordEncoder，cashier-server 必须统一。

**Independent Test**: 在 cashier-server 注册新商户或修改密码后，直接查询数据库 `cashier_merchants` 表的密码字段，验证不是 32 位十六进制 MD5 字符串，而是 bcrypt 格式（`$2a$...`）。

**Acceptance Scenarios**:

1. **Given** 现有商户密码为 MD5 哈希，**When** 系统升级后商户首次登录，**Then** 系统兼容验证旧 MD5 密码，并在验证通过后自动将密码升级为 bcrypt 存储。
2. **Given** 新商户注册或修改密码，**When** 密码写入数据库，**Then** 存储格式为 bcrypt（`$2a$` 开头）。
3. **Given** admin-server 已使用 `BCryptPasswordEncoder`，**When** cashier-server 接入 bcrypt，**Then** 两个服务的密码哈希策略一致。

---

### User Story 4 - 前端 API 调用统一与错误处理 (Priority: P2)

作为开发者，前端所有 API 调用应通过统一的 Axios 实例，享受自动 401 拦截、响应解包和统一错误处理。对账相关页面必须有错误提示。

**Why this priority**: 5 处直接使用 `fetch()` 绕过 Axios 拦截器，导致 401 未跳转登录、响应未解包。4 个对账页面完全没有 `catch` 错误处理，API 失败时用户看不到任何提示。

**Independent Test**: 临时关闭后端服务，访问对账任务页面，验证页面显示"加载失败"错误提示而非白屏或永久加载状态。

**Acceptance Scenarios**:

1. **Given** 管理员已登录，**When** 下载订单 CSV，**Then** 使用统一的 Axios 实例而非原生 fetch()，自动携带 Authorization 头。
2. **Given** admin-server 返回 401，**When** 任何 API 调用（包括对账页面的 fetch），**Then** 前端统一跳转到登录页。
3. **Given** 对账任务 API 返回错误，**When** 加载任务列表，**Then** 页面显示 ElMessage 错误提示，列出具体错误原因。
4. **Given** 对账结果/汇总/异常 API 返回错误，**When** 加载对应页面，**Then** 同样有明确的错误提示。

---

### User Story 5 - JWT 安全增强 (Priority: P2)

作为系统安全管理员，需要支持 JWT 主动失效（登出）和 Token 刷新机制，当用户登出或检测到异常时能立即撤销 Token。

**Why this priority**: 当前 JWT 没有任何失效机制——没有登出接口、没有黑名单、没有刷新 Token。Token 被窃取后只能等 24 小时过期，期间攻击者可任意操作。

**Independent Test**: 管理员登录后调用登出 API，验证返回的 JWT 被加入 Redis 黑名单，再次使用该 JWT 访问受保护接口时返回 401。

**Acceptance Scenarios**:

1. **Given** 管理员持有有效 JWT，**When** 调用 `POST /api/v1/admin/auth/logout`，**Then** 服务端将该 JWT 加入 Redis 黑名单（TTL = Token 剩余有效期），返回成功。
2. **Given** JWT 已在黑名单中，**When** 使用该 JWT 访问受保护接口，**Then** `JwtInterceptor` 检查黑名单后返回 401。
3. **Given** admin-server 和 cashier-server 各自维护独立的登出端点，**When** 用户登出，**Then** 各自的 JWT 被独立撤销。

---

### User Story 6 - 生产环境配置安全 (Priority: P2)

作为运维人员，项目必须提供 `application-prod.yml` 配置文件模板，确保生产部署时数据库密码、JWT 密钥、内部令牌等敏感配置通过环境变量注入，而非硬编码在配置文件中。

**Why this priority**: 当前无生产配置 profile，所有默认值（`root:root` 数据库密码、`default_jwt_secret`、MyBatis SQL 日志输出到 stdout）在默认 profile 中都是危险的。

**Independent Test**: 设置环境变量后启动 `--spring.profiles.active=prod`，验证数据库密码从环境变量读取而非 `root:root`，MyBatis SQL 日志不输出。

**Acceptance Scenarios**:

1. **Given** 设置了 `DB_PASSWORD`、`JWT_SECRET`、`INTERNAL_TOKEN` 等环境变量，**When** 以 prod profile 启动服务，**Then** 所有敏感配置从环境变量读取，不依赖配置文件的默认值。
2. **Given** prod profile 激活，**When** MyBatis 执行 SQL 查询，**Then** SQL 语句不被打印到 stdout。
3. **Given** prod profile 激活，**When** 启动任一 server，**Then** `spring.sql.init.mode` 为 `never`，不会自动执行 schema.sql。

---

### User Story 7 - 前端国际化完善 (Priority: P3)

作为国际化用户，前端页面的所有文本应支持中英文切换，而非硬编码中文。

**Why this priority**: 当前 i18n 仅覆盖 34 个菜单和通知键值，其他数百个 UI 字符串全部硬编码中文。cashier-client 完全没有 i18n。

**Independent Test**: 切换语言为英文，验证管理后台的主要页面（订单列表、渠道管理、商户管理）仍显示为主的英文文本。

**Acceptance Scenarios**:

1. **Given** admin-client 语言设置为 en-US，**When** 浏览订单列表页面，**Then** 表头、按钮、状态标签显示英文。
2. **Given** cashier-client 集成 i18n，**When** 用户访问收银台页面，**Then** 金额、支付方式名称等正确显示中文。

---

### User Story 8 - 数据库迁移工具引入 (Priority: P3)

作为开发者，数据库 Schema 变更必须可追溯、可回滚、自动执行，而非依赖手动 SQL 脚本。

**Why this priority**: 当前迁移靠手动执行 SQL 文件，无版本追踪。6 个迁移文件命名格式不一致（有的用 `-`，有的不用）。Flyway 是 Spring Boot 生态的标准方案。

**Independent Test**: 在空白数据库上启动服务，验证 Flyway 按版本顺序自动执行所有迁移脚本，数据库结构与预期一致。

**Acceptance Scenarios**:

1. **Given** 空白数据库，**When** 启动各 server 的 prod profile，**Then** Flyway 自动执行所有历史迁移，创建完整表结构。
2. **Given** 新增功能需要 DDL 变更，**When** 开发者创建 Flyway 迁移文件（`V{N}__description.sql`），**Then** 下次启动自动执行，`flyway_schema_history` 记录版本。

---

### User Story 9 - 补充单元测试与集成测试 (Priority: P3)

作为开发者，核心业务逻辑必须有自动化测试覆盖，防止回归。

**Why this priority**: 整个项目仅 2 个非标准测试文件。CI 脚本显式跳过测试（`-DskipTests`）。支付核心逻辑零测试覆盖。

**Independent Test**: 运行 `mvn test`，验证至少 payflow-common 和 payflow-payment-core 模块的测试通过。

**Acceptance Scenarios**:

1. **Given** payflow-common 的 `AesEncryptor`，**When** 加密后解密，**Then** 得到原始文本——用单元测试验证。
2. **Given** `PayStrategyRegistry` 注入了所有策略 Bean，**When** 按支付方式代码查找策略，**Then** 返回正确的策略实现——用 SpringBootTest 验证。
3. **Given** CI 工作流运行，**When** 执行 `mvn test`，**Then** 测试结果反馈到 CI 状态（不再跳过测试）。

---

### User Story 10 - 补充 API 文档 (Priority: P3)

作为对接开发者，管理后台的 API 接口必须有 OpenAPI 文档，方便前端开发和第三方对接。

**Why this priority**: admin-server 的 26 个 Controller 完全没有任何 SpringDoc 注解。只有 cashier-server 的 7 个 Controller 有 `@Tag` 和 `@Operation`。

**Independent Test**: 启动 admin-server 后访问 `/swagger-ui.html`，验证至少能看到用户管理、订单管理、渠道管理等主要模块的 API 文档。

**Acceptance Scenarios**:

1. **Given** admin-server 添加了 `springdoc-openapi-starter-webmvc-ui` 依赖，**When** 启动后访问 `/api-docs`，**Then** 返回完整的 OpenAPI JSON。
2. **Given** 新增了 `OpenAPI` Bean 配置，**When** 查看 Swagger UI，**Then** 显示项目标题"PayFlow 管理后台 API"和服务版本。

---

### User Story 11 - 服务监控与健康检查 (Priority: P3)

作为运维人员，所有三个后端服务必须有健康检查端点和基础监控指标。

**Why this priority**: 只有 admin-server 有 Actuator。cashier-server 和 recon-server 完全没有健康检查，K8s 或负载均衡器无法判断服务是否存活。

**Independent Test**: 启动 cashier-server 后访问 `/actuator/health`，验证返回 `{"status":"UP"}`。

**Acceptance Scenarios**:

1. **Given** cashier-server 添加了 Actuator 依赖，**When** 访问 `/actuator/health`，**Then** 返回服务状态（包含 Redis、MySQL 连接检查）。
2. **Given** recon-server 添加了 Actuator 依赖，**When** 访问 `/actuator/health`，**Then** 返回服务状态。

---

### User Story 12 - Docker 容器化支持 (Priority: P3)

作为运维人员，所有后端服务必须提供 Dockerfile，支持容器化部署。

**Why this priority**: 项目完全没有 Docker 支持，无法在 Kubernetes 或 Docker Compose 环境中部署。这是现代化部署的基础。

**Independent Test**: 在项目根目录执行 `docker build -f Dockerfile -t payflow-admin-server .` 并启动容器，验证服务正常运行。

**Acceptance Scenarios**:

1. **Given** 存在 `Dockerfile`（多阶段构建：Maven 编译 + JRE 运行），**When** 构建镜像，**Then** 产生可运行的容器镜像。
2. **Given** `docker-compose.yml` 包含 MySQL、Redis、三个后端服务，**When** 执行 `docker-compose up`，**Then** 整个系统可一键启动。

---

### Edge Cases

- MD5 密码升级为 bcrypt 时，如何处理存量数据？→ 首次登录用旧 MD5 验证，验证通过后自动升级为 bcrypt。
- Flyway 迁移引入后，如何处理已存在的数据库？→ 使用 Flyway baseline，将基线版本设为当前已应用的最后一个手动迁移。
- 微信 APIv3 密钥从数据库读取，如果读取失败（网络/权限），重试 3 次后记录告警日志并返回失败，不阻塞其他商户的回调。
- JWT 黑名单存 Redis，如果 Redis 不可用，此时应拒绝请求（fail-close）还是放行（fail-open）？→ 出于安全考虑，fail-close：Redis 不可用时拒绝所有请求，防止黑名单检查被绕过。

## Requirements

### Functional Requirements

- **FR-001**: 系统 MUST 在收到微信支付回调时正确解密资源、验签、更新订单状态，不再抛出 `UnsupportedOperationException`。
- **FR-002**: 系统 MUST 在生产环境中将 RuntimeException 和 Exception 异常信息记录到日志但不返回给前端，前端仅看到通用错误信息。
- **FR-003**: cashier-server MUST 使用 bcrypt 存储商户密码，并兼容存量 MD5 密码的自动升级。
- **FR-004**: 系统 MUST 提供 JWT 登出/撤销机制，将失效 Token 加入 Redis 黑名单，TTL 等于 Token 剩余有效时间。
- **FR-005**: 系统 MUST 提供 `application-prod.yml` 模板，所有敏感配置通过环境变量注入，MyBatis SQL 日志在 prod 环境关闭。
- **FR-006**: admin-client 的 5 处 `fetch()` 调用 MUST 迁移为 Axios 调用，享受统一的拦截器处理。
- **FR-007**: 对账 4 个页面 MUST 添加 catch 错误处理，API 失败时显示 ElMessage 错误提示。
- **FR-008**: admin-client MUST 将硬编码中文 UI 字符串抽取到 i18n 文件中；cashier-client MUST 集成 vue-i18n。
- **FR-009**: 所有三个后端服务 MUST 引入 Flyway 管理数据库迁移，历史 SQL 脚本按版本顺序集成。
- **FR-010**: payflow-common 和 payflow-payment-core MUST 至少拥有覆盖核心功能的单元测试。
- **FR-011**: admin-server MUST 添加 SpringDoc 依赖和 `@Tag`/`@Operation` 注解到主要 Controller。
- **FR-012**: cashier-server 和 recon-server MUST 添加 Actuator 健康检查端点。
- **FR-013**: 三个后端服务 MUST 各自提供 Dockerfile（多阶段构建），项目根目录 MUST 提供 `docker-compose.yml`。
- **FR-014**: CI 工作流 MUST 执行测试而非跳过（移除 `-DskipTests`），并在合并结果中反映测试通过/失败。

### Key Entities

- **JwtBlacklist**: Redis 键值，key = `jwt:blacklist:{jti}`，value = 失效原因，TTL = Token 剩余有效期。用于实现登出和强制下线。
- **Flyway Migration History**: 由 Flyway 自动管理的 `flyway_schema_history` 表，记录已执行迁移的版本、描述、执行时间和校验和。
- **Docker Compose Services**: MySQL 8.0、Redis 7、admin-server:3003、cashier-server:3002、recon-server:3004、admin-client:80。

## Success Criteria

### Measurable Outcomes

- **SC-001**: 微信支付回调成功率从 0%（当前因 UnsupportedOperationException 全部失败）提升到 100%（假设渠道侧正常）。
- **SC-002**: 所有 API 错误响应不再包含 Java 异常类名、堆栈跟踪或内部文件路径。
- **SC-003**: 商户密码在数据库中的存储格式 100% 为 bcrypt（新密码和已升级的旧密码）。
- **SC-004**: JWT 登出后，原 Token 在下次请求时（不超过 5 秒延迟）被拒绝。
- **SC-005**: 生产部署时，无任何敏感值（密码、密钥、令牌）以明文形式出现在配置文件或 stdout 中。
- **SC-006**: 前端所有页面在 API 失败时均显示用户可见的错误提示，无静默失败。
- **SC-007**: admin-client 主要页面（≥80% 的 UI 字符串）支持中英文切换。
- **SC-008**: `mvn test` 命令能执行并通过所有核心模块的测试，CI 不再跳过测试步骤。

## Clarifications

### Session 2026-05-10

- Q: JWT 黑名单按什么做 Token 唯一标识？当前 JwtUtils 未生成 jti → A: 生成 Token 时添加 `jti`（UUID），黑名单按 jti 精确撤销单个 Token

## Assumptions

- 微信 APIv3 密钥将存储在现有渠道账户 JSON 配置中（`channelConfig` 字段），不需要额外的密钥管理方案。
- 生产环境部署将使用 Docker Compose 或 K8s，故 Dockerfile 基于标准 JRE 镜像。
- Flyway 引入后，现有手动 SQL 脚本作为 baseline，后续增量变更使用 Flyway 管理。
- i18n 实际范围：admin-client 覆盖主要 CRUD 页面（订单、渠道、商户、用户），cashier-client 覆盖收银台主页面。
- 测试范围：优先覆盖 common 模块的加密工具和 payment-core 的策略注册表，不要求全量测试覆盖。
- JWT 黑名单依赖 Redis 可用性——Redis 不可用时，安全策略为 fail-close（拒绝所有请求）。
