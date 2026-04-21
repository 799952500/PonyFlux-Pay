# PonyFlux-Pay — MyBatis 绑定错误修复

**时间**：2026-04-20 18:20 GMT+8
**问题**：Invalid bound statement (not found): com.payflow.admin.mapper.AdminUserMapper.selectList

---

## 根本原因

pplication.yml 配置了 SQL 初始化（mode: always），Spring Boot 会加载两套 SQL 文件：
- esources/schema.sql + data.sql — **旧 H2 格式**，默认加载路径
- esources/sql/admin-schema.sql + dmin-data.sql — **新版 MySQL 格式**

两套**同时执行**，旧 H2 的 dmin_users 表没有 password 列，导致 MyBatis-Plus 查询时字段找不到。

## 已修复

1. 删除 esources/schema.sql 和 esources/data.sql（旧 H2 残留）
2. 从 pom.xml 移除 H2 依赖（仅保留 mysql-connector-j）
3. mvn clean compile → BUILD SUCCESS（49 个源文件）

## 待验证

启动后访问登录接口：
`
POST http://localhost:3003/api/v1/auth/login
Body: { "username": "admin", "password": "admin123" }
`

**预期**：返回 JWT token