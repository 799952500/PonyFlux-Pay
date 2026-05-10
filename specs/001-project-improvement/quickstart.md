# Quickstart: 项目健壮性改进开发指南

## 开发环境准备

```bash
# 确保已有环境
java -version          # Java 17+
mvn -version           # Maven 3.8+
node -v                # Node 20+
docker -v              # Docker (for Docker story)
mysql -V               # MySQL 8.0
redis-cli ping         # Redis 7
```

## 分支与工作区

```bash
git checkout 001-project-improvement
```

## 按故事开发顺序

### Phase 1: P1 阻断性修复 (优先)

#### Story 1 — 微信回调修复

```bash
# 修改文件
# payflow-payment-wechat/.../WxPayNotifyHelper.java
# payflow-cashier-server/.../service/impl/PaymentServiceImpl.java (回调处理)

# 验证: 启动 cashier-server，发送模拟回调
mvn -B -pl payflow-cashier-server spring-boot:run
# Postman: POST http://localhost:3002/notify/wechat
```

#### Story 2 — 异常安全

```bash
# 修改文件
# payflow-cashier-server/.../exception/GlobalExceptionHandler.java
# payflow-recon-server/.../exception/GlobalExceptionHandler.java
# 20+ BizException 包装处（搜索 "e.getMessage()" 在 BizException 构造中）

# 验证: 制造 RuntimeException，检查响应
curl http://localhost:3002/api/v1/orders/notexist
# 应返回 {"code":500,"message":"服务器内部错误"}
```

#### Story 3 — 密码安全

```bash
# 引入依赖: spring-boot-starter-security (仅 BCrypt)
# 修改: AuthServiceImpl.java

# 验证: 用旧 MD5 密码登录，检查数据库密码已升级为 $2a$...
python scripts/verify_admin_password.py
```

### Phase 2: P2 重要改进

#### Story 4 — 前端统一

```bash
cd payflow-admin-client
# 修改: src/api/request.ts (fetch → axios)
# 修改: src/pages/admin/reconcile/*.vue (add catch)
# 修改: src/pages/admin/onboarding.vue (add catch)
npm run dev  # 验证对账页面错误提示
```

#### Story 5 — JWT 安全

```bash
# JwtUtils.java: 添加 jti
# JwtInterceptor.java: 检查 Redis 黑名单
# AuthController.java: 新增 POST /auth/logout
curl -X POST http://localhost:3003/api/v1/admin/auth/logout \
  -H "Authorization: Bearer <token>"
```

#### Story 6 — 生产配置

```bash
# 新增 application-prod.yml 到三个 server
# 验证
mvn -B -pl payflow-admin-server spring-boot:run -Dspring-boot.run.profiles=prod
# 检查: MyBatis 无 SQL 日志, 密码来自 $DB_PASSWORD
```

### Phase 3: P3 基础完善

#### Story 7 — i18n
```bash
cd payflow-admin-client
# 修改 locales/zh-CN.ts + en-US.ts
# 替换 .vue 中的硬编码中文为 $t()
```

#### Story 8 — Flyway
```bash
# 第一步: baseline (非破坏性)
# 直接启动，Flyway 自动 baseline + 后续 versioned migration
mvn -B -pl payflow-admin-server spring-boot:run
# 检查 flyway_schema_history 表创建
```

#### Story 9 — 测试
```bash
# 新增测试
mvn -B -pl payflow-common,payflow-payment-core test
```

#### Story 10 — API 文档
```bash
# admin-server 添加 @Tag/@Operation
mvn -B -pl payflow-admin-server spring-boot:run
# 访问 http://localhost:3003/swagger-ui.html
```

#### Story 11 — 健康检查
```bash
# cashier + recon 添加 Actuator
curl http://localhost:3002/actuator/health
curl http://localhost:3004/actuator/health
```

#### Story 12 — Docker
```bash
docker build -f payflow-admin-server/Dockerfile -t payflow-admin .
docker compose up -d
```

## 常用验证命令

```bash
# 全量编译
mvn -B compile

# 运行测试
mvn -B test

# 启动全部服务 (docker)
docker compose up -d

# 检查健康状态
curl http://localhost:3002/actuator/health
curl http://localhost:3003/actuator/health
curl http://localhost:3004/actuator/health
```
