# Research: 项目健壮性改进计划

**Date**: 2026-05-10
**Feature**: [spec.md](./spec.md)

## Decision Records

### D1: JWT jti 生成策略

- **Decision**: 在 `JwtUtils.generateToken()` 中添加 `jti`（JWT ID）声明，值为 `UUID.randomUUID().toString()`，黑名单 key = `jwt:blacklist:{jti}`。
- **Rationale**: 每个 Token 有全局唯一 ID，支持精确撤销单个 Token。扩展性好——未来可支持"撤销某用户所有 Token"（Redis Set 存 user:{userId}:tokens → jti 列表）。
- **Alternatives considered**:
  - Token 哈希作为 key → 同样能唯一标识，但无法关联用户维度操作。
  - 用户黑名单时间戳 → 粒度太粗，只能全部撤销，不符合"单独登出"需求。

### D2: bcrypt 实现方案

- **Decision**: 引入 `spring-boot-starter-security`（仅取 `BCryptPasswordEncoder`），不启用 Spring Security 过滤器链。在 `AuthServiceImpl` 中加入 MD5 兼容逻辑：先用 MD5 比对 → 成功则用 bcrypt 重写密码 → 后续用 bcrypt 验证。
- **Rationale**: `BCryptPasswordEncoder` 是 Spring 生态标准，admin-server 已使用。单独引入 `jbcrypt` 库也可以，但会增加一个新依赖，不如复用 Spring Security。
- **Alternatives considered**:
  - 引入 standalone `jbcrypt` → 更轻量但增加新依赖管理。
  - 强制所有商户重置密码 → 用户体验差，且需要额外的重置流程。

### D3: Flyway 多数据库迁移

- **Decision**: 三个 server 各自管理自己的 Flyway 迁移。目录结构：
  - `payflow-admin-server/src/main/resources/db/migration/admin/` — payflow_admin 库的表
  - `payflow-cashier-server/src/main/resources/db/migration/cashier/` — payflow_cashier 库的表
  - `payflow-recon-server/src/main/resources/db/migration/recon/` — payflow_admin 库的 recon_* 表
  首次迁移使用 `baselineOnMigrate: true` + `baselineVersion: 1`。
- **Rationale**: 每个 server 的启动独立，各自负责自己管理的数据库 schema。避免单点 Flyway 管理多库带来的排序依赖问题。Recon-server 虽然操作 `payflow_admin` 库，但其只管理 `recon_*` 表，通过不同 `flyway.table` 避免与 admin-server 的 migration 表冲突。
- **Alternatives considered**:
  - 单点 Flyway → 需要一个"主人模块"管理所有迁移，启动顺序耦合。
  - Liquibase → 功能更丰富但 Spring Boot 默认集成不如 Flyway 简单。

### D4: 微信 APIv3 密钥缓存

- **Decision**: 从 `ChannelConfigHolder.getChannelConfig()` JSON 中提取 `apiV3Key` 字段。首次获取后缓存到本地 `ConcurrentHashMap`（key = 渠道账户 ID），通过 Redis Pub/Sub (`payflow:cashier:config:refresh`) 失效。与现有 `OrderCacheService` 的刷新模式一致。
- **Rationale**: 每次回调都查数据库不可接受（微信高峰期回调 QPS 高）。缓存方案确保低延迟，且与项目已有的配置刷新模式一致。
- **Alternatives considered**:
  - 每次从数据库加载 → 延迟高，但实现最简单。仅当回调 QPS 很低时可行。
  - 只启动时加载 → 密钥变更需重启，不够灵活。

### D5: Docker 多阶段构建

- **Decision**: 每个后端服务一个 `Dockerfile`，使用 Maven 编译阶段 + JRE 运行阶段：
  ```dockerfile
  FROM maven:3.9-eclipse-temurin-17 AS build
  COPY . /src
  WORKDIR /src
  RUN mvn -B -pl {module} -am -DskipTests package

  FROM eclipse-temurin:17-jre
  COPY --from=build /src/{module}/target/*.jar /app.jar
  ENTRYPOINT ["java", "-jar", "/app.jar"]
  ```
  `docker-compose.yml` 编排：MySQL 8.0、Redis 7、3 个后端服务、1 个 Nginx（serve admin-client dist）。
- **Rationale**: 多阶段构建减小最终镜像体积。Maven 构建复用 Docker 层缓存。Nginx 直接 serve 前端构建产物，无需 Node 运行时。
- **Alternatives considered**:
  - Jib Maven Plugin → 无需 Dockerfile，但不够透明且不便于调试。

### D6: i18n 实现策略

- **Decision**: admin-client 使用现有 `vue-i18n`，按页面维度组织翻译键值（`orders.*`, `channels.*`, `merchants.*`）。cashier-client 新增 `vue-i18n`。使用 `t('key')` 函数替换硬编码中文。所有新增/修改的键值同时在 `zh-CN.ts` 和 `en-US.ts` 中定义。
- **Rationale**: `vue-i18n` 已在 admin-client 安装。按页面分组避免键值冲突，方便维护。
- **Alternatives considered**:
  - 仅保留中文、不做翻译 → 影响国际化用户，但工作量最小。可接受为短期策略。

### D7: 异常处理安全策略

- **Decision**: cashier-server 和 recon-server 的 `GlobalExceptionHandler.handleRuntimeException()` 改为返回 `R.serverError("服务器内部错误")`（固定字符串），异常详情仅记录到 `log.error()`。`BizException` 的 message 保留（这些是业务层主动抛出的用户可读错误信息）。但需修复 20+ 处 `BizException` 包装位置——底层异常 message 不应拼接到 BizException message 中，而是作为 cause 传递。
- **Rationale**: BizException 的 message 由开发者编写，通常适合展示给用户（如"订单不存在"）。但 chat 过程中发现的 20+ 处拼接 `e.getMessage()` 会泄露内部信息。修复方式：BizException 构造时将原始异常作为 `Throwable cause` 传入，message 保持业务语义。
- **Alternatives considered**:
  - BizException 也统一返回通用消息 → 过度防御，丢失业务语义。

## Dependency Additions

| Module | New Dependency | Purpose |
|--------|---------------|---------|
| payflow-cashier-server | `spring-boot-starter-security` (only BCrypt) | 密码哈希 |
| payflow-cashier-server | `flyway-core` + `flyway-mysql` | 数据库迁移 |
| payflow-cashier-server | `spring-boot-starter-actuator` | 健康检查 |
| payflow-admin-server | `flyway-core` + `flyway-mysql` | 数据库迁移 |
| payflow-admin-server | `springdoc-openapi-starter-webmvc-ui` | API 文档 |
| payflow-recon-server | `flyway-core` + `flyway-mysql` | 数据库迁移 |
| payflow-recon-server | `spring-boot-starter-actuator` | 健康检查 |
| payflow-cashier-client | `vue-i18n` | 国际化 |

## No-Go Decisions

- **不引入 Spring Security 过滤器链**：仅使用 BCryptPasswordEncoder，不添加 `@EnableWebSecurity`，保持现有拦截器安全模型。
- **不做 Token 刷新机制**：当前优先级 P2，登出+黑名单已满足基本安全需求。Token 刷新留待后续独立迭代。
- **不修改 UnionPay 策略**：银联占位模块不是本次改进范围，属于新功能开发。
- **不引入 Prometheus/ Micrometer**：仅添加 Actuator health 端点。
