# [CHECKLIST TYPE] Checklist: [FEATURE NAME]

**Purpose**: [此检查清单覆盖的内容]
**Created**: [DATE]
**Feature**: [链接到 spec.md]

**Note**: 本检查清单由 `/speckit-checklist` 命令根据功能上下文和需求生成。

---

## 编码规范

- [ ] CHK001 所有类名使用 UpperCamelCase，方法/变量使用 lowerCamelCase
- [ ] CHK002 常量使用 UPPER_SNAKE_CASE，枚举后缀 Enum
- [ ] CHK003 无通配符导入（`import java.util.*`）
- [ ] CHK004 大括号 K&R 风格，即使单语句也有 `{}`
- [ ] CHK005 单行 ≤ 120 字符，4 空格缩进
- [ ] CHK006 公共类和方法有 Javadoc 注释
- [ ] CHK007 POJO 属性使用包装类型，无默认值
- [ ] CHK008 使用构造器注入（`@RequiredArgsConstructor`），禁止 `@Autowired` 字段注入

## 安全规范

- [ ] CHK009 敏感字段（密钥/密码）使用 AES-256-GCM 加密存储
- [ ] CHK010 日志中无敏感信息（手机号脱敏 `139****1219`、密码/密钥完全屏蔽）
- [ ] CHK011 请求包含时间戳 + Nonce 防重放校验（±5 分钟窗口）
- [ ] CHK012 支付接口有幂等性保护（Redis 锁 / 数据库唯一约束）
- [ ] CHK013 Controller 入参 DTO 使用 Bean Validation（`@NotBlank`、`@Size` 等）
- [ ] CHK014 文件上传有类型和大小检查
- [ ] CHK015 CORS 白名单配置正确，未同时使用 `"*"` 和 `allowCredentials(true)`

## SQL 与数据库

- [ ] CHK016 XML 中无 `${}`（仅 ORDER BY 且白名单校验后例外）
- [ ] CHK017 无 `SELECT *`，分页查询设置 maxLimit（≤ 500）
- [ ] CHK018 实体类标注 `@TableName`、`@TableId`、`@Version`、`@TableLogic`
- [ ] CHK019 Long ID 使用 `@JsonSerialize(using = ToStringSerializer.class)`
- [ ] CHK020 批量操作单次 ≤ 500 条，多表 JOIN ≤ 3 张表
- [ ] CHK021 新增表遵循对应数据库前缀约定（admin_/cashier_/recon_）

## API 规范

- [ ] CHK022 Controller 返回统一格式 `{ code: 0, message: "success", data: {...} }`
- [ ] CHK023 错误码使用对应模块的范围（1xxx 管理/2xxx 支付/4xxx 退款/7500-7599 对账）
- [ ] CHK024 全局异常处理器统一返回格式，无堆栈信息泄露
- [ ] CHK025 `docs/CONTRACT_MATRIX.md` 已更新，包含新增 API

## 模块边界

- [ ] CHK026 代码位于正确的 Maven 模块中（对照 CLAUDE.md 模块表）
- [ ] CHK027 payflow-common 中无 Spring Bean、业务逻辑或实体类
- [ ] CHK028 支付渠道无直接注入具体 Handler（使用 Locator/Registry）
- [ ] CHK029 无跨 Maven 模块的直接实体类引用（渠道模块不引用 cashier/admin 实体）

## 测试

- [ ] CHK030 新增业务逻辑有对应单元测试（JUnit 5 + Mockito）
- [ ] CHK031 Definition of Done 五项条件全部满足
- [ ] CHK032 JaCoCo 行覆盖率 ≥ 80%（模块聚合）
- [ ] CHK033 核心支付流程有集成测试（Testcontainers）

## 文档

- [ ] CHK034 CLAUDE.md 中模块列表、端口号、技术版本与实现一致
- [ ] CHK035 数据库迁移 SQL 已在 `sql/migrations/` 和 `sql/full-reseed-payflow-demo.sql` 中

## Notes

- 完成检查的项目标记为 `[x]`
- 不适用本功能的项目标记为 `[x] N/A` 并注明原因
- 违反[强制]级别的项目必须在合并前修复
- 违反[推荐]级别的项目需要在 Review 中说明豁免理由
