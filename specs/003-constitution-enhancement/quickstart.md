# Quick Start: 增强后宪法使用指南

**Date**: 2026-05-13
**Audience**: PonyFlux-Pay 项目开发者

---

## 1. 宪法是什么

`.specify/memory/constitution.md` 是 PonyFlux-Pay 项目的最高编码准则。它定义了：
- **必须遵守**（强制）：违反后 CI 不通过、PR 不能合并
- **应该遵守**（推荐）：不遵守需在 Code Review 中说明理由
- **可以参考**（参考）：提供上下文和最佳实践引导

## 2. 快速定位

| 我想知道... | 看这个章节 |
|-------------|-----------|
| 实体类怎么写？ | 数据库访问规范 → 实体类规范 |
| Controller 返回什么格式？ | 核心原则 IV. API 响应规范 |
| 金额用什么类型？ | 技术约束 → 金额约定 |
| 商户密钥怎么存？ | 核心原则 V. 密钥与配置安全 |
| 线程池怎么创建？ | 集合与并发处理 → 线程池规范 |
| 日志怎么打？ | 异常与日志规范 |
| 测试覆盖率多少？ | 测试规范 → 覆盖率要求 |
| 我的 PR 达到 DoD 了吗？ | 测试规范 → Definition of Done |
| 前端组件怎么命名？ | 前端规范 → Vue 3 编码规范 |

## 3. 开发流程中的宪法介入点

```
写代码 → IDE 插件实时检查（Alibaba Coding Guidelines）
  ↓
提交前 → Git pre-commit hook（Checkstyle 快速检查）
  ↓
创建 PR → CI 流水线全面检查（Checkstyle + SonarQube + JaCoCo）
  ↓
Code Review → Reviewer 依据宪法 Review Checklist 逐项检查
  ↓
合并 → 所有门禁通过 + 测试覆盖率 ≥ 80% + DoD 五项条件
```

## 4. 使用 /speckit 命令时的宪法引导

| 命令 | 涉及模板 | 宪法引导内容 |
|------|----------|-------------|
| `/speckit-specify` | spec-template.md | Constitution Compliance 段落引导声明功能涉及的宪法原则 |
| `/speckit-plan` | plan-template.md | Constitution Check 自动列出需检查的 5 大原则 |
| `/speckit-tasks` | tasks-template.md | 任务自动按模块边界分组（原则 I） |
| `/speckit-checklist` | checklist-template.md | 包含编码/安全/测试/API/模块边界检查项 |
| `/speckit-constitution` | constitution-template.md | 新项目宪法自动包含所有章节占位 |

## 5. 常见违规示例

| 违规代码 | 宪法条款 | 正确写法 |
|----------|----------|----------|
| `@Autowired private Service s;` | 编码规范-依赖注入 | `private final Service s;` + `@RequiredArgsConstructor` |
| `SELECT * FROM cashier_orders` | 数据库访问-查询规范 | `SELECT id, order_no, amount FROM cashier_orders` |
| `password = "123456"` | 安全编码-硬编码 | 使用环境变量 + AES-256-GCM 加密 |
| `ExecutorService pool = Executors.newFixedThreadPool(10);` | 集合与并发-线程池 | `new ThreadPoolExecutor(...)` 显式指定参数 |
| `log.info("用户{}支付{}元", phone, amount);` | 异常与日志-脱敏 | `log.info("用户{}支付{}元", maskPhone(phone), amount);` |

## 6. 工具安装清单

- [ ] IntelliJ IDEA 安装 **Alibaba Java Coding Guidelines** 插件
- [ ] IntelliJ IDEA 安装 **Checkstyle-IDEA** 插件，导入 `checkstyle.xml`
- [ ] IDE 启用 **Save Actions** 自动格式化（Google Java Format 或项目自定义规则）
- [ ] IDE 启用 **JaCoCo** 覆盖率 runner
- [ ] VS Code（前端）安装 **ESLint** + **Prettier** 插件
