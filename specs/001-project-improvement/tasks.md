# Tasks: 项目健壮性改进计划

**Input**: Design documents from `/specs/001-project-improvement/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: 仅 P3-US9（单元测试）包含测试任务。其他故事不包含测试任务。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 所属用户故事（US1-US12）
- 包含精确文件路径

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 引入所有故事需要的依赖和配置基础

- [x] T001 [P] 在 payflow-cashier-server/pom.xml 中添加 `flyway-core`、`flyway-mysql`、`spring-boot-starter-actuator`、`spring-boot-starter-security` 依赖
- [x] T002 [P] 在 payflow-admin-server/pom.xml 中添加 `flyway-core`、`flyway-mysql`、`springdoc-openapi-starter-webmvc-ui` 依赖
- [x] T003 [P] 在 payflow-recon-server/pom.xml 中添加 `flyway-core`、`flyway-mysql`、`spring-boot-starter-actuator` 依赖
- [x] T004 在 payflow-cashier-client 中安装 vue-i18n: `cd payflow-cashier-client && npm install vue-i18n`

---

## Phase 2: Foundational（阻塞性前置任务）

**Purpose**: 所有用户故事启动前必须完成的基础设施

**⚠️ CRITICAL**: 以下任务完成前，不能开始任何用户故事

- [x] T005 [P] 为 payflow-cashier-server 创建 Flyway baseline 迁移文件 payflow-cashier-server/src/main/resources/db/migration/cashier/V1__baseline.sql（空文件，Flyway baselineOnMigrate 模式）
- [x] T006 [P] 为 payflow-admin-server 创建 Flyway baseline 迁移文件 payflow-admin-server/src/main/resources/db/migration/admin/V1__baseline.sql（空文件）
- [x] T007 [P] 为 payflow-recon-server 创建 Flyway baseline 迁移文件 payflow-recon-server/src/main/resources/db/migration/recon/V1__baseline.sql（空文件），配置 `flyway.table=flyway_recon_schema_history`
- [x] T008 [P] 在 payflow-cashier-server/src/main/resources/application.yml 中添加 Flyway、Actuator、bcrypt 配置
- [x] T009 [P] 在 payflow-admin-server/src/main/resources/application.yml 中添加 Flyway、SpringDoc 配置
- [x] T010 [P] 在 payflow-recon-server/src/main/resources/application.yml 中添加 Flyway、Actuator 配置
- [x] T011 创建 payflow-cashier-server/src/main/resources/application-prod.yml（生产环境配置模板：环境变量注入、关闭 MyBatis SQL 日志、sql.init.mode=never）
- [x] T012 [P] 创建 payflow-admin-server/src/main/resources/application-prod.yml
- [x] T013 [P] 创建 payflow-recon-server/src/main/resources/application-prod.yml

**Checkpoint**: 基础设施就绪——现在可以并行开始各用户故事

---

## Phase 3: User Story 1 — 微信支付回调修复 (Priority: P1) 🎯 MVP

**Goal**: WxPayNotifyHelper 实现 APIv3 密钥读取，微信支付回调能正常处理

**Independent Test**: 启动 cashier-server，向 `/notify/wechat` 发送模拟回调，验证订单状态更新

### Implementation for US1

- [x] T014 [US1] 在 payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayNotifyHelper.java 中实现 `getWxPayApiV3Key()`：从 `ChannelConfigHolder.getChannelConfig()` JSON 中提取 `apiV3Key` 字段
- [x] T015 [US1] 在 payflow-cashier-server 中创建 payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/wxpay/WxPayApiV3KeyCache.java，使用 `ConcurrentHashMap` 缓存密钥（key=渠道账户ID），通过 Redis Pub/Sub `payflow:cashier:config:refresh` 失效
- [x] T016 [US1] 修改 payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayNotifyHelper.java，`decryptResource()` 方法调用 `WxPayApiV3KeyCache` 获取密钥而非抛出 `UnsupportedOperationException`
- [x] T017 [US1] 在 PayStrategyRegistry.dispatchChannelNotify() 中添加微信回调验签失败时的详细错误日志

**Checkpoint**: 微信支付回调可正常处理，订单状态自动更新

---

## Phase 4: User Story 2 — 异常信息不泄露 (Priority: P1)

**Goal**: Runtime/Exception 不泄露内部信息到前端；BizException 包装时不拼接底层异常 message

**Independent Test**: 制造 RuntimeException，调用 API 验证返回 "服务器内部错误" 而非 NPE 详情

### Implementation for US2

- [x] T018 [US2] 修改 payflow-cashier-server/src/main/java/com/payflow/cashier/exception/GlobalExceptionHandler.java：`handleRuntimeException()` 返回固定 `R.serverError("服务器内部错误")`，异常详情仅记录 `log.error()`
- [x] T019 [US2] 修改 payflow-recon-server/src/main/java/com/payflow/recon/exception/GlobalExceptionHandler.java：同上
- [x] T020 [US2] 修改 payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayV3HttpClient.java：BizException 构造时将原始异常作为 cause 传入，message 保留业务语义（如 "微信支付API异常(Native下单)"），不在 message 中拼接 `e.getMessage()`
- [x] T021 [P] [US2] 修改 payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayNativeHandler.java：BizException 不再拼接 `e.getMessage()`（第 68、119 行）
- [x] T022 [P] [US2] 修改 payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayAppHandler.java：同上（第 111 行）
- [x] T023 [P] [US2] 修改 payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayH5Handler.java：同上（第 107 行）
- [x] T024 [P] [US2] 修改 payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayJsapiHandler.java：同上（第 72、100 行）
- [x] T025 [P] [US2] 修改 payflow-payment-wechat/src/main/java/com/payflow/payment/wechat/WxPayMicropayHandler.java：同上（第 59 行）
- [x] T026 [P] [US2] 修改 payflow-payment-alipay/src/main/java/com/payflow/payment/alipay/AliPayAppHandler.java：同上（第 48 行）
- [x] T027 [P] [US2] 修改 payflow-payment-alipay/src/main/java/com/payflow/payment/alipay/AliPayWapHandler.java：同上（第 50 行）
- [x] T028 [P] [US2] 修改 payflow-payment-alipay/src/main/java/com/payflow/payment/alipay/AliPayQrHandler.java：同上（第 59、102 行）
- [x] T029 [P] [US2] 修改 payflow-payment-alipay/src/main/java/com/payflow/payment/alipay/AliPayBarcodeHandler.java：同上（第 46 行）
- [x] T030 [P] [US2] 修改 payflow-recon-server 中所有 BizException 构造处：`WxPayBillService.java`、`ReconMerchantStatementService.java`、`ReconExecuteService.java`、`AlipayReconChannelOpenService.java`、`WxpayReconChannelOpenService.java`——BizException 不拼接 `e.getMessage()`
- [x] T031 [US2] 修改 payflow-cashier-server 中 `RiskQlEvaluator.java`：风控表达式执行异常的 BizException 不拼接 `e.getMessage()`

**Checkpoint**: 所有 RuntimeException 返回通用消息；BizException 的 message 仅含业务语义文本

---

## Phase 5: User Story 3 — 密码安全存储 (Priority: P1)

**Goal**: cashier-server 使用 bcrypt 存储商户密码，兼容存量 MD5 自动升级

**Independent Test**: 注册新商户 → 查数据库密码为 `$2a$...` 格式；旧 MD5 密码商户首次登录后密码自动升级

### Implementation for US3

- [x] T032 [US3] 修改 payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/AuthServiceImpl.java：`login()` 方法中先用 `BCryptPasswordEncoder.matches()` 验证，若失败且数据库密文长度=32（MD5），则用 MD5 比对，成功后 `BCryptPasswordEncoder.encode()` 写回数据库
- [x] T033 [US3] 在 payflow-cashier-server 中添加 `BCryptPasswordEncoder` Bean（在 config 包或直接注入），确保不创建 `@EnableWebSecurity`，不影响现有拦截器认证
- [x] T034 [US3] 在 payflow-cashier-server 密码修改/注册流程中，确保新密码使用 `BCryptPasswordEncoder.encode()` 存储

**Checkpoint**: 所有商户密码存储格式统一为 bcrypt，旧密码自动升级

---

## Phase 6: User Story 4 — 前端 API 调用统一与错误处理 (Priority: P2)

**Goal**: 所有 `fetch()` 调用改为 Axios；对账 4 个页面 + onboarding 页面添加 catch 错误处理

**Independent Test**: 关闭后端服务，访问对账任务页面验证显示错误提示

### Implementation for US4

- [x] T035 [US4] 修改 payflow-admin-client/src/api/admin.ts：`getLoginFeatures()`（第 34 行）从 `fetch()` 改为 Axios 调用
- [x] T036 [P] [US4] 修改 payflow-admin-client/src/api/admin.ts：`exportOrdersCsv()`（第 93 行）从 `fetch()` 改为 Axios，处理文件下载响应
- [x] T037 [P] [US4] 修改 payflow-admin-client/src/api/admin.ts：`getMetaVersion()`（第 126 行）从 `fetch()` 改为 Axios
- [x] T038 [US4] 修改 payflow-admin-client/src/router/index.ts：路由守卫中的 `fetch('/api/v1/admin/auth/profile')` 改为 Axios
- [x] T039 [US4] 修改 payflow-admin-client/src/pages/login/index.vue：验证码 `fetch()` 改为 Axios
- [x] T040 [P] [US4] 修改 payflow-admin-client/src/pages/admin/reconcile/tasks.vue：`loadTasks()`、`loadDiffs()`、`downloadFile()`、`submitManual()`、`rerun()` 添加 `.catch()` 错误处理，用 ElMessage.error 显示提示
- [x] T041 [P] [US4] 修改 payflow-admin-client/src/pages/admin/reconcile/results.vue：`load()` 添加 catch + ElMessage.error
- [x] T042 [P] [US4] 修改 payflow-admin-client/src/pages/admin/reconcile/summary.vue：`load()` 和 `loadAnomalies()` 添加 catch + ElMessage.error
- [x] T043 [P] [US4] 修改 payflow-admin-client/src/pages/admin/reconcile/index.vue：若有 API 调用则添加 catch（按实际代码确定）
- [x] T044 [P] [US4] 修改 payflow-admin-client/src/pages/admin/onboarding.vue：`listOnboardingApplications()` 添加 catch + ElMessage.error
- [x] T045 [P] [US4] 修改 payflow-admin-client/src/pages/admin/notifications.vue：catch 中改为 ElMessage.error 提示而非静默设 0

**Checkpoint**: 所有前端 API 调用统一走 Axios；对账页面有完整错误提示

---

## Phase 7: User Story 5 — JWT 安全增强 (Priority: P2)

**Goal**: JWT 添加 jti 声明；实现登出接口 + Redis 黑名单；JwtInterceptor 校验黑名单

**Independent Test**: 登录 → 获取 Token → 登出 → 旧 Token 被拒绝返回 401

### Implementation for US5

- [x] T046 [US5] 修改 payflow-admin-server/src/main/java/com/payflow/admin/util/JwtUtils.java：`generateToken()` 中添加 `jti` 声明（UUID.randomUUID().toString()）
- [x] T047 [P] [US5] 修改 payflow-cashier-server/src/main/java/com/payflow/cashier/util/JwtUtils.java：`generateToken()` 中添加 `jti` 声明
- [x] T048 [US5] 在 payflow-admin-server/src/main/java/com/payflow/admin/controller/AuthController.java 中添加 `POST /api/v1/admin/auth/logout` 端点：解析当前 JWT 的 jti → Redis `SET jwt:blacklist:{jti} "logout" EX {剩余秒数}` → 返回成功
- [x] T049 [P] [US5] 在 payflow-cashier-server/src/main/java/com/payflow/cashier/controller/MerchantAuthController.java 中添加 `POST /api/v1/auth/logout` 端点：同样逻辑，解析当前 Token 的 jti 加入黑名单
- [x] T050 [US5] 修改 payflow-admin-server/src/main/java/com/payflow/admin/interceptor/JwtInterceptor.java：解析 Token 后，检查 `Redis EXISTS jwt:blacklist:{jti}` → 存在则返回 401 `{"code":1401,"message":"Token已失效"}`
- [x] T051 [P] [US5] 修改 payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/JwtAuthInterceptor.java：同样检查黑名单
- [x] T052 [US5] 在 JwtInterceptor/JwtAuthInterceptor 中：Redis 不可用时（连接超时/异常）→ fail-close，返回 401 "认证服务暂不可用"
- [x] T053 [US5] 确保 admin-client 前端路由守卫在收到 401 后跳转到登录页（已由 Axios 拦截器实现）

**Checkpoint**: JWT 支持登出撤销，黑名单机制生效

---

## Phase 8: User Story 6 — 生产环境配置安全 (Priority: P2)

**Goal**: 三服务提供 application-prod.yml，MyBatis SQL 日志在 prod 关闭，spring.sql.init.mode=never

**Independent Test**: 以 prod profile 启动 → 验证数据库密码来自环境变量、无 SQL 日志输出

### Implementation for US6

- [x] T054 [US6] 完善 payflow-cashier-server/src/main/resources/application-prod.yml：配置 `spring.datasource.password: ${DB_PASSWORD}`、`payflow.jwt.secret: ${JWT_SECRET}`、`payflow.signature.secret: ${SIGNATURE_SECRET}`、`payflow.crypto.master-key: ${MASTER_KEY}`
- [x] T055 [P] [US6] 完善 payflow-admin-server/src/main/resources/application-prod.yml：配置 `spring.datasource.password`、`spring.cashier-datasource.password`、`jwt.secret`、内部 Token 等均从环境变量读取
- [x] T056 [P] [US6] 完善 payflow-recon-server/src/main/resources/application-prod.yml：同上，配置 `payflow.recon.internal-token` 从环境变量读取
- [x] T057 [US6] 在三个 application-prod.yml 中设置 `mybatis-plus.configuration.log-impl:` 为空或 `org.apache.ibatis.logging.nologging.NoLoggingImpl`（关闭 SQL 日志）
- [x] T058 [US6] 在 payflow-cashier-server/src/main/resources/application-prod.yml 中设置 `spring.sql.init.mode: never`
- [x] T059 [US6] 将三个 server 的 application.yml 中的硬编码默认敏感值（JWT secret、internal token）替换为 `${ENV_VAR:dev-default}` 占位符形式

**Checkpoint**: prod profile 启动无明文敏感配置和 SQL 日志

---

## Phase 9: User Story 7 — 前端国际化完善 (Priority: P3)

**Goal**: admin-client 覆盖重点 CRUD 页面（订单、渠道、商户、用户）；cashier-client 集成 vue-i18n

**Independent Test**: 切换语言为英文 → 验证订单列表表头、按钮、状态标签显示英文

### Implementation for US7

- [x] T060 [US7] 扩展 payflow-admin-client/src/locales/zh-CN.ts：新增 `orders.*`、`channels.*`、`merchants.*`、`users.*` 翻译键值（表头、按钮、状态标签、表单标签）
- [x] T061 [P] [US7] 扩展 payflow-admin-client/src/locales/en-US.ts：对应英文翻译
- [x] T062 [US7] 替换 payflow-admin-client/src/pages/admin/orders/index.vue 和 orders/detail.vue 中硬编码中文为 `$t('orders.*')`
- [x] T063 [P] [US7] 替换 payflow-admin-client/src/pages/admin/channels.vue 中硬编码中文为 `$t('channels.*')`
- [x] T064 [P] [US7] 替换 payflow-admin-client/src/pages/admin/merchants.vue 中硬编码中文为 `$t('merchants.*')`
- [x] T065 [P] [US7] 替换 payflow-admin-client/src/pages/admin/users.vue 中硬编码中文为 `$t('users.*')`
- [x] T066 [P] [US7] 替换 payflow-admin-client/src/pages/admin/refunds.vue 中状态列标签为 `$t()`
- [x] T067 [P] [US7] 替换 payflow-admin-client/src/pages/admin/dashboard.vue 中 KPI 卡片标题为 `$t()`
- [x] T068 [US7] 在 payflow-cashier-client/src/main.ts 中引入 vue-i18n，创建 `src/locales/zh-CN.ts` 和 `src/locales/en-US.ts`
- [x] T069 [US7] 替换 payflow-cashier-client/src/pages/cashier/index.vue 中硬编码文本为 `$t()`
- [x] T070 [P] [US7] 替换 payflow-cashier-client/src/pages/login/index.vue 中硬编码文本为 `$t()`

**Checkpoint**: 管理后台主要页面支持中英文切换

---

## Phase 10: User Story 8 — 数据库迁移工具 Flyway (Priority: P3)

**Goal**: 三服务启动时 Flyway 自动管理迁移；存量手动 SQL 脚本作为 baseline

**Independent Test**: 空白数据库启动服务 → Flyway 创建 flyway_schema_history 表 + 标记 baseline

### Implementation for US8

- [x] T071 [US8] 将 sql/migrations/ 下现有 6 个迁移脚本整合为 Flyway versioned migration，命名 `V2__` 到 `V7__`，放入对应服务目录。确认命名格式统一（`V{序号}__{描述}.sql`）
- [x] T072 [US8] 在 payflow-cashier-server/src/main/resources/application.yml 中配置 Flyway：`spring.flyway.locations=classpath:db/migration/cashier`，`spring.flyway.baseline-on-migrate=true`，`spring.flyway.baseline-version=1`
- [x] T073 [P] [US8] 在 payflow-admin-server/src/main/resources/application.yml 中配置 Flyway：`spring.flyway.locations=classpath:db/migration/admin`
- [x] T074 [P] [US8] 在 payflow-recon-server/src/main/resources/application.yml 中配置 Flyway：`spring.flyway.locations=classpath:db/migration/recon`，`spring.flyway.table=flyway_recon_schema_history`
- [x] T075 [US8] 验证：依次启动三个 server（dev profile），确认 flyway_schema_history 表正确创建且 baseline 成功

**Checkpoint**: Flyway 自动管理数据库迁移，增量变更走 versioned migration

---

## Phase 11: User Story 9 — 补充单元测试 (Priority: P3)

**Goal**: payflow-common（AesEncryptor）和 payflow-payment-core（PayStrategyRegistry）有测试覆盖

**Independent Test**: `mvn -pl payflow-common,payflow-payment-core test` 通过

### Tests for US9

- [x] T076 [P] [US9] 创建 payflow-common/src/test/java/com/payflow/common/crypto/AesEncryptorTest.java：测试加密后解密返回原文、错误密钥解密失败、空字符串加密
- [x] T077 [P] [US9] 创建 payflow-payment-core/src/test/java/com/payflow/payment/core/PayStrategyRegistryTest.java：Mock `List<PayStrategy>`，验证 `requireByCode()` 正确返回策略、不存在的 code 抛异常

### Implementation for US9

- [x] T078 [US9] 在 .github/workflows/ci.yml 中移除 `-DskipTests`，改为 `mvn -B -q test`

**Checkpoint**: `mvn test` 可执行并有基础测试通过

---

## Phase 12: User Story 10 — 补充 API 文档 (Priority: P3)

**Goal**: admin-server 所有主要 Controller 添加 @Tag 和 @Operation 注解

**Independent Test**: 启动 admin-server → 访问 `/swagger-ui.html` → 看到订单管理、商户管理等模块

### Implementation for US10

- [x] T079 [US10] 在 payflow-admin-server 中创建 `OpenAPIConfig` Bean：设置标题 "PayFlow 管理后台 API"、版本号
- [x] T080 [US10] 为 payflow-admin-server/.../controller/AdminOrderController.java 添加 `@Tag(name = "订单管理")` + 每个方法 `@Operation(summary = "...")`
- [x] T081 [P] [US10] 为 AdminRefundController.java 添加 `@Tag(name = "退款管理")` + `@Operation`
- [x] T082 [P] [US10] 为 AdminChannelController.java, AdminChannelAccountController.java, AdminChannelRouteController.java 添加 `@Tag` + `@Operation`
- [x] T083 [P] [US10] 为 AdminMerchantController.java, MerchantPaymentMethodController.java, MerchantPaymentRouteController.java 添加 `@Tag` + `@Operation`
- [x] T084 [P] [US10] 为 SysUserController.java, SysRoleController.java, SysMenuController.java 添加 `@Tag` + `@Operation`
- [x] T085 [P] [US10] 为 AuthController.java, AdminDashboardController.java, SystemConfigController.java 添加 `@Tag` + `@Operation`
- [x] T086 [P] [US10] 为 AdminReconController.java, AdminRiskController.java 添加 `@Tag` + `@Operation`

**Checkpoint**: Swagger UI 可浏览 admin-server 所有 API 接口

---

## Phase 13: User Story 11 — 服务监控与健康检查 (Priority: P3)

**Goal**: cashier-server 和 recon-server 提供 `/actuator/health` 端点

**Independent Test**: `curl http://localhost:3002/actuator/health` 返回 `{"status":"UP"}`

### Implementation for US11

- [x] T087 [US11] 在 payflow-cashier-server/src/main/resources/application.yml 中配置 Actuator：`management.endpoints.web.exposure.include=health,info`，`management.endpoint.health.show-details=when-authorized`
- [x] T088 [P] [US11] 在 payflow-recon-server/src/main/resources/application.yml 中配置 Actuator：同上
- [x] T089 [US11] 验证：启动 cashier-server、recon-server → `curl /actuator/health` 返回 UP

**Checkpoint**: 三个服务均有健康检查端点

---

## Phase 14: User Story 12 — Docker 容器化支持 (Priority: P3)

**Goal**: 三后端服务各有 Dockerfile（多阶段构建）；docker-compose.yml 编排全栈

**Independent Test**: `docker compose up -d` → 三个服务启动 → `/actuator/health` 返回 UP

### Implementation for US12

- [x] T090 [US12] 创建 payflow-cashier-server/Dockerfile：多阶段构建（maven:3.9-eclipse-temurin-17 → eclipse-temurin:17-jre）
- [x] T091 [P] [US12] 创建 payflow-admin-server/Dockerfile：同上
- [x] T092 [P] [US12] 创建 payflow-recon-server/Dockerfile：同上
- [x] T093 [US12] 创建项目根目录 docker-compose.yml：包含 mysql:8.0、redis:7、admin-server:3003、cashier-server:3002、recon-server:3004 服务，配置网络、数据卷、环境变量
- [x] T094 [US12] 创建 .dockerignore 文件：排除 node_modules、.git、target、logs
- [x] T095 [US12] 验证：`docker compose up -d` → 等待健康检查通过 → `curl localhost:3002/actuator/health` 等三个服务均返回 UP

**Checkpoint**: 一键 `docker compose up` 启动全系统

---

## Phase 15: Polish & Cross-Cutting Concerns

**Purpose**: 跨故事优化和清理

- [x] T096 [P] 删除 payflow-admin-client/src/api/channel.js、channel-api.js、merchant-api.js、payment-method.js 中未被引用的旧 API 文件（验证无 import 引用后）
- [x] T097 [P] 删除 payflow-cashier-server/src/main/resources/application-test.yml 和 application-sandbox.yml 中不一致的配置（若与 prod 模板重复则合并）
- [x] T098 更新 docs/CONTRACT_MATRIX.md：补充 AdminReconController、AdminInsightsController、AdminMerchantOnboardingController 的 API 映射
- [x] T099 验证全量编译：`mvn -B compile` 通过（9 模块）
- [x] T100 运行 quickstart.md 中的验证命令确认功能正常

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖，立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成 → **阻塞所有用户故事**
- **US1-US3 (P1)**: 依赖 Foundational 完成，三者可**并行**
- **US4-US6 (P2)**: 依赖 Foundational 完成，三者可并行；US5 依赖 US3 的 BCrypt Bean（弱依赖：可并行但最终合并）
- **US7-US12 (P3)**: 依赖 Foundational 完成，可并行

### User Story Dependencies

- **US1 (微信回调)**: 独立，可并行
- **US2 (异常安全)**: 独立，可并行
- **US3 (密码安全)**: 独立，可并行
- **US4 (前端统一)**: 独立，可并行
- **US5 (JWT 安全)**: 弱依赖 US3（共用 BCrypt Bean），建议排在 US3 之后
- **US6 (生产配置)**: 独立，可并行
- **US7 (i18n)**: 独立，可并行
- **US8 (Flyway)**: 独立，可并行
- **US9 (测试)**: 独立，可并行
- **US10 (API 文档)**: 独立，可并行
- **US11 (健康检查)**: 独立，可并行
- **US12 (Docker)**: 独立，可并行

### Within Each User Story

- 后端：公共接口/工具修复 → 业务逻辑 → 配置
- 前端：API 层修复 → 页面修复 → 插件安装

---

## Parallel Opportunities

### Phase 1 (Setup) — 全部 4 个 Task 可并行
```
T001 (cashier POM)  ∥  T002 (admin POM)  ∥  T003 (recon POM)  ∥  T004 (npm install)
```

### Phase 2 (Foundational) — 9 个 Task，按依赖分两组
```
第一组（并行）: T005 ∥ T006 ∥ T007 ∥ T008 ∥ T009 ∥ T010
第二组（并行）: T011 ∥ T012 ∥ T013
```

### Phase 3-5 (P1 Stories) — 3 个故事可并行
```
Story 1 (T014-T017)  ∥  Story 2 (T018-T031)  ∥  Story 3 (T032-T034)
```

### Phase 4 (US2 内部) — BizException 修复
```
T021 ∥ T022 ∥ T023 ∥ T024 ∥ T025 ∥ T026 ∥ T027 ∥ T028 ∥ T029 ∥ T030 (不同文件，全部可并行)
```

---

## Implementation Strategy

### MVP First (P1 Stories Only)

1. Complete Phase 1: Setup（依赖安装）
2. Complete Phase 2: Foundational（Flyway baseline、配置模板）
3. Complete Phase 3: US1（微信回调修复）
4. **STOP and VALIDATE**: 测试微信回调、验证订单更新
5. Complete Phase 4: US2（异常安全）
6. Complete Phase 5: US3（密码安全）
7. **DEPLOY MVP**: P1 修复完毕，阻断性 bug 全部解决

### Incremental Delivery

1. Setup + Foundational → 基础设施就绪
2. P1 故事（US1-US3）→ 阻断性修复 → 可部署
3. P2 故事（US4-US6）→ 安全/体验增强 → 可部署
4. P3 故事（US7-US12）→ 工程化完善 → 可部署
5. 每个阶段独立测试、独立部署、不破坏已部署功能

### Recommended Execution Order (Sequential by Priority)

如果单人开发：

```
T001-T013 (Setup + Foundational) → T014-T017 (US1) → T018-T031 (US2) → T032-T034 (US3)
→ T035-T045 (US4) → T046-T053 (US5) → T054-T059 (US6)
→ T060-T070 (US7) → T071-T075 (US8) → T076-T078 (US9)
→ T079-T086 (US10) → T087-T089 (US11) → T090-T095 (US12)
→ T096-T100 (Polish)
```

---

## Notes

- [P] 任务 = 不同文件、无依赖，可同时执行
- [Story] 标签映射任务到具体用户故事，便于追踪
- 每个用户故事可独立完成和测试
- 每个任务或逻辑组完成后提交
- 在 Checkpoint 处停止验证故事独立性
- P1 故事完成后即可获得项目最关键的改进价值
