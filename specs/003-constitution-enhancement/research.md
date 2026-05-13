# Research: 项目宪法增强 — 规范来源研究

**Date**: 2026-05-13
**Purpose**: 调查业界成熟的 Java 开发规范，筛选最适合 PonyFlux-Pay 项目的条目，为宪法章节设计提供依据。

---

## 1. 规范来源评估

### 来源 1: 阿里巴巴 Java 开发手册（泰山版/嵩山版/黄山版）

- **地位**: 国内 Java 开发事实标准，GitHub 50k+ Stars
- **覆盖领域**: 编程规约、异常日志、单元测试、安全规约、MySQL 数据库、工程结构、设计规约
- **约束力分级**: 强制/推荐/参考（本项目直接采纳此分级体系）

**Decision**: 采纳为核心规范来源。泰山版为基础（6 大维度），嵩山版补充前后端规约和 JDK 17 特性，黄山版强化 Lambda/Stream 和云原生。

**Rationale**: 国内最成熟的 Java 规范体系，与本书项目技术栈（Java 17 + Spring Boot）高度匹配，且提供 IDEA 插件可直接落地。

**Alternatives considered**: 《Effective Java》(Joshua Bloch) — 权威但缺少国内特有的数据库和工程结构规范；Google Java Style Guide — 仅覆盖代码风格，不涉及架构和安全。

---

### 来源 2: Google Java Style Guide

- **地位**: Java 代码风格的国际化标准
- **覆盖领域**: 文件结构、缩进格式、命名规范、Javadoc、导入声明
- **特点**: 2 空格缩进、K&R 大括号、禁止通配符导入

**Decision**: 部分采纳（命名规范、注释规范、Lambda 格式），但缩进风格保留项目传统（4 空格）。

**Rationale**: 命名规范和 Javadoc 规则成熟度高，直接采纳可减少争议。缩进风格（2 vs 4 空格）属于低级偏好，保留项目现状 4 空格即可。

**Alternatives considered**: Sun Java Style Guide — 过时；Spring Framework Code Style — 未独立发布，分散在 Spring 源码中。

---

### 来源 3: Spring Boot 3 生产级最佳实践

- **来源**: Spring 官方文档、Pro Spring Boot 3 (Apress 2025)、spring-boot-best-practices (GitHub)
- **关键要点**:
  - 构造器注入（`@RequiredArgsConstructor`）为强制，禁止字段注入
  - `@RestControllerAdvice` 集中异常处理
  - DTO ↔ Entity 分离，使用 MapStruct
  - RFC 9457 Problem Details for HTTP APIs
  - Testcontainers 用于集成测试
  - Actuator 端点安全配置
  - 结构化 JSON 日志 + TraceId

**Decision**: 全部采纳。这些是最佳实践，且与项目当前架构一致。

**Rationale**: 项目已使用 Spring Boot 3.2.5，采纳这些实践为零成本。构造器注入和 DTO 分离已有部分基础，主要是规范化。

**Alternatives considered**: 无有效替代方案，Spring Boot 生态无竞品框架。

---

### 来源 4: MyBatis-Plus 深度开发规范

- **来源**: MyBatis-Plus 官方文档、掘金社区实践总结
- **关键要点**:
  - 实体类必须：`@TableName`、`@TableId(IdType.ASSIGN_ID)`、`@Version`、`@TableLogic`
  - 分页插件 `PaginationInnerInterceptor` 设 `maxLimit`
  - `BlockAttackInnerInterceptor` 防全表更新/删除
  - Lambda 方式引用列名（`User::getUserName`）
  - XML 中禁用 `${}`（仅 `ORDER BY` 例外）
  - 批量操作单次 ≤ 500 条
  - 多表 JOIN ≤ 3 张表

**Decision**: 全部采纳为强制规则。

**Rationale**: 项目使用 MyBatis-Plus 3.5.7 作为 ORM，这些规则直接防止 SQL 注入、性能下降和数据误操作。

**Alternatives considered**: MyBatis 原生 XML 规范 — 部分重叠，但未覆盖 MyBatis-Plus 特有功能。

---

### 来源 5: 支付系统安全最佳实践

- **来源**: PCI DSS 基础要求、金融级安全实践、CSDN 支付系统设计指南
- **关键要点**:
  - AES-256-GCM 加密敏感字段
  - 日志脱敏（手机号/身份证/银行卡号）
  - 请求签名 + 时间戳防重放
  - 幂等性设计（Redis 锁 / 数据库唯一约束）
  - 三层防御（传输/应用/数据）
  - 密钥管理（KMS / 环境变量）

**Decision**: 全部采纳，其中 AES-256-GCM 和日志脱敏为强制规则。

**Rationale**: 支付系统涉及资金安全，安全规范不能妥协。项目已有 `AesEncryptor` 基础，补充脱敏和防重放要求即可。

**Alternatives considered**: 国密 SM2/SM4 — 不强制，作为可选推荐。

---

## 2. 规范筛选原则

针对本项目（PonyFlux-Pay 支付网关）的特点，筛选采用三级优先级：

| 优先级 | 适用场景 | 示例 |
|--------|----------|------|
| **P0 强制采纳** | 支付安全、SQL 注入防护、资金数据精度 | AES-256-GCM、禁止 `${}`、金额用 Long 分 |
| **P1 强烈推荐** | 可维护性、性能、一致性 | 构造器注入、DTO 分离、禁止 `SELECT *` |
| **P2 按需参考** | 特定场景、团队偏好 | Stream 代替 for 循环、Optional 用法 |

**核心原则**: 每条采纳的规则必须可自动化检查或人工验证。不能落地的规则暂不纳入宪法。

---

## 3. 研究结论

### 宪法新增章节清单（8 个新章节 + 5 个增强现有章节）

| # | 章节名称 | 主要来源 | 强制规则数（估计） |
|---|----------|----------|-------------------|
| 1 | 编码规范 | Alibaba + Google | ~15 |
| 2 | 集合与并发处理 | Alibaba | ~8 |
| 3 | 数据库访问规范 | MyBatis-Plus + Alibaba | ~12 |
| 4 | 安全编码规范 | 支付安全 + Alibaba | ~10 |
| 5 | 异常与日志规范 | Alibaba + Spring Boot 3 | ~8 |
| 6 | 测试规范 | Spring Boot 3 Best Practices | ~6 |
| 7 | 自动化执行 | Alibaba 插件 + Checkstyle | ~4 |
| 8 | 前端规范 | Vue 3 + TypeScript Best Practices | ~10 |
| - | 现有 5 个原则增强 | 项目实践反馈 | ~5（新增案例） |

**总计**: 约 73 条强制规则 + 若干推荐/参考规则。宪法文件预计 600-800 行。

### 模板文件改造方案

| 模板文件 | 改造内容 |
|----------|----------|
| `spec-template.md` | 新增 "Constitution Compliance" 段落，引导开发者列出功能涉及的宪法原则 |
| `plan-template.md` | Constitution Check 从占位符替换为基于实际原则的 5+ 项检查清单 |
| `tasks-template.md` | 任务分组建议按模块边界（payflow-common/core/channels/cashier/admin/recon） |
| `checklist-template.md` | 检查类别新增：编码规范、安全规范、测试覆盖率、API 规范、模块边界 |
| `constitution-template.md` | 章节结构更新为包含上述新章节的占位说明 |
