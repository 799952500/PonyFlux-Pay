# Data Model: 项目宪法结构设计

**Date**: 2026-05-13
**Purpose**: 定义增强后宪法的章节结构、模板文件改动点、以及各章节之间的关联关系。

---

## 1. 宪法章节结构（目标状态）

```
.specify/memory/constitution.md
├── # PonyFlux-Pay 项目宪法
├── ## 核心原则（保留 + 增强）
│   ├── I. 模块边界纪律（增强：增加正例/反例代码片段）
│   ├── II. 支付渠道抽象（增强：增加渠道 Handler 注入违规示例）
│   ├── III. 数据库分区（增强：增加 MyBatis-Plus 双数据源配置要点）
│   ├── IV. API 响应规范（增强：增加前端解包流程说明）
│   └── V. 密钥与配置安全（增强：增加日志脱敏要求）
│
├── ## 编码规范 [新增]
│   ├── 命名规范（类/方法/变量/常量/包/枚举）
│   ├── 代码格式（缩进4空格/K&R大括号/单行120字符）
│   ├── 注释规范（Javadoc/行注释/TODO规范）
│   ├── 类成员顺序（常量→静态变量→实例变量→构造器→公共方法→私有方法）
│   └── POJO 规范（包装类型/禁止默认值/序列化要求）
│
├── ## 集合与并发处理 [新增]
│   ├── 集合初始化（指定初始容量/Arrays.asList限制）
│   ├── 集合遍历（禁止foreach中修改/使用Iterator或removeIf）
│   ├── 线程池规范（禁止Executors/必须用ThreadPoolExecutor）
│   ├── ThreadLocal 清理（finally中remove）
│   └── 并发工具选择（优先java.util.concurrent）
│
├── ## 数据库访问规范 [新增]
│   ├── 实体类规范（@TableName/@TableId/@Version/@TableLogic）
│   ├── 查询规范（禁止SELECT */分页上限/Lambda引用列名）
│   ├── SQL 安全（禁止${}/参数绑定/防全表更新删除）
│   ├── 批量操作（saveBatch限制500条）
│   ├── JOIN 限制（不超过3张表）
│   └── 逻辑删除（手写SQL需手动加deleted=0）
│
├── ## 安全编码规范 [新增]
│   ├── 数据加密（AES-256-GCM/商户密钥加密存储/主密钥环境变量注入）
│   ├── 日志脱敏（手机号/身份证/银行卡号/密码脱敏规则）
│   ├── 防重放与幂等性（时间戳+Nonce/Redis分布式锁）
│   ├── 参数校验（Bean Validation/白名单校验）
│   ├── SQL 注入与 XSS 防护
│   └── 文件上传安全（类型检查/大小限制）
│
├── ## 异常与日志规范 [新增]
│   ├── 异常处理（全局异常处理器/BizException/错误码分配）
│   ├── 日志级别（ERROR/WARN/INFO/DEBUG使用场景）
│   ├── 日志内容（禁止打印敏感信息/必须包含TraceId）
│   └── 日志框架（Logback/结构化JSON日志）
│
├── ## 测试规范 [新增]
│   ├── Definition of Done（5项条件全部满足才算完成）
│   ├── 覆盖率要求（80%行覆盖率/按模块聚合/JaCoCo配置）
│   ├── 单元测试（JUnit5+Mockito/禁止Mock数据库）
│   ├── 集成测试（Testcontainers/@SpringBootTest使用场景）
│   └── 前端测试（Vitest+Vue Test Utils/API Mock策略）
│
├── ## 自动化执行 [新增]
│   ├── IDE 插件（Alibaba Java Coding Guidelines/Checkstyle-IDEA）
│   ├── CI 流水线（Checkstyle/SonarQube/JaCoCo门禁）
│   └── Git Hooks（pre-commit/pre-push检查策略）
│
├── ## 前端规范 [新增]
│   ├── Vue 3 编码规范（组件命名/Composition API优先）
│   ├── TypeScript 类型规范（类型定义/禁止any滥用）
│   ├── Pinia 状态管理规范（Store命名/拆分策略）
│   ├── API 调用规范（Axios拦截器/错误处理/响应解包）
│   └── 前端测试规范（Vitest配置/覆盖率80%）
│
├── ## 技术约束（保留，更新版本号引用）
├── ## 开发工作流（保留，增加测试门禁步骤）
└── ## 治理（保留，增强合规审查部分增加Review Checklist）
```

---

## 2. 每条规则的内部结构（JSON Schema 概念）

```text
Rule {
  id: String           // 如 "DB-001"
  title: String        // 如 "禁止 SELECT *"
  severity: enum       // MANDATORY | RECOMMENDED | REFERENCE
  description: String  // 规则说明
  rationale: String    // 违反后果
  positive_example: String  // 正例代码（Markdown代码块）
  negative_example: String  // 反例代码（Markdown代码块）
  check_method: String      // 检查方式（Checkstyle/SonarQube/人工Review）
}
```

---

## 3. 模板文件改动关系

```text
spec-template.md
  └── 新增: ## Constitution Compliance 段落
       └── 列出功能涉及的原则 → 映射到 plan.md 的 Constitution Check

plan-template.md
  └── 替换: Constitution Check 占位符
       └── 展开为 5+ 项基于宪法实际原则的检查清单（带勾选框）

tasks-template.md
  └── 替换: Phase 拆分建议
       └── 按模块边界分组：payflow-common | payflow-core | payflow-channels |
           payflow-cashier | payflow-admin | payflow-recon | 前端

checklist-template.md
  └── 新增: 检查类别
       └── 编码规范 | 安全规范 | 测试覆盖率 | API 规范 | 模块边界 | 文档更新

constitution-template.md
  └── 更新: 章节占位
       └── 与目标宪法结构对齐（见上文 §1）
```

---

## 4. 规则约束力级别定义

| 级别 | 标识 | 违反后果 | 典型规则 |
|------|------|----------|----------|
| **强制** | `MANDATORY` | CI 阻断，PR 不得合并 | 禁止 `${}`、禁止 `SELECT *`、密钥加密 |
| **推荐** | `RECOMMENDED` | Review 中需说明豁免理由 | 构造器注入、MapStruct 映射 |
| **参考** | `REFERENCE` | 提供上下文指导，不做强制 | Stream vs for 循环、Optional 用法 |
