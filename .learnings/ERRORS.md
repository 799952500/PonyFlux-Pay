# Errors

## [ERR-20260521-003] cashier_resource_ownership_test_compile

**Logged**: 2026-05-21T15:24:14+08:00
**Priority**: high
**Status**: pending
**Area**: backend

### Summary
运行收银端资源归属测试时，cashier-server 在 main compile 阶段因 UnionPay 依赖类解析失败而中断。

### Error
```text
mvn -B -pl payflow-cashier-server -Dtest=ResourceOwnershipServiceTest test

D:\个人\pay\PonyFlux-Pay\payflow-cashier-server\src\main\java\com\payflow\cashier\sdk\unionpay\UnionPayNotifyHelper.java:[15,32] 错误: 找不到符号
  符号:   类 UnionPayApiConstants
  位置: 程序包 com.payflow.payment.union
D:\个人\pay\PonyFlux-Pay\payflow-cashier-server\src\main\java\com\payflow\cashier\sdk\strategy\UnionH5Strategy.java:[10,32] 错误: 找不到符号
  符号:   类 UnionPayAccountConfig
  位置: 程序包 com.payflow.payment.union
D:\个人\pay\PonyFlux-Pay\payflow-cashier-server\src\main\java\com\payflow\cashier\sdk\strategy\UnionQrStrategy.java:[12,32] 错误: 找不到符号
  符号:   类 UnionPayQrHandler
  位置: 程序包 com.payflow.payment.union
```

### Context
- 操作：为 T027 增强 `ResourceOwnershipServiceTest` 后运行模块单测。
- 输入：`mvn -B -pl payflow-cashier-server -Dtest=ResourceOwnershipServiceTest test`
- 结果：测试尚未进入执行阶段，失败发生在 cashier-server 主代码编译。
- 当前变更文件：`payflow-cashier-server/src/test/java/com/payflow/cashier/service/ResourceOwnershipServiceTest.java`

### Suggested Fix
检查 cashier-server 对 `payflow-payment-channels/payflow-payment-union` 的 Maven 依赖链，或使用 `-am` 同时构建所需模块；若类已改包/改名，修正 cashier-server 中 UnionPay strategy/helper 的 import。

### Metadata
- Reproducible: yes
- Related Files: payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/strategy/UnionH5Strategy.java, payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/strategy/UnionQrStrategy.java, payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/unionpay/UnionPayNotifyHelper.java
- See Also: none

---

## [ERR-20260521-002] agent_explore_model_not_found

**Logged**: 2026-05-21T05:43:00+08:00
**Priority**: medium
**Status**: pending
**Area**: config

### Summary
调用 Explore 子代理研究代码库时因模型通道不可用失败。

### Error
```text
API Error: 503 {"error":{"code":"model_not_found","message":"No available channel for model deepseek-v4-flash under group Codex-Paul (distributor) ...","type":"new_api_error"}}
```

### Context
- 操作：Agent 工具调用 `subagent_type=Explore`，用于研究商户数据隔离影响范围
- 结果：子代理未执行任何工具调用，返回 model_not_found
- 处理：改用本地 Glob/Grep/Read 直接探索代码库

### Suggested Fix
当 Agent/Explore 因模型不可用失败时，不要重复相同调用；改用直接搜索工具或换用可用子代理配置。

### Metadata
- Reproducible: unknown
- Related Files: specs/008-merchant-data-isolation/plan.md
- See Also: none

---

## [ERR-20260521-001] read_tool_pages_parameter

**Logged**: 2026-05-21T00:00:00+08:00
**Priority**: medium
**Status**: pending
**Area**: config

### Summary
Read 工具读取普通文本文件时错误传入空 `pages` 参数导致调用失败。

### Error
```text
Invalid pages parameter: "". Use formats like "1-5", "3", or "10-20". Pages are 1-indexed.
```

### Context
- 操作：读取 `.specify/extensions.yml`、`.specify/templates/spec-template.md`、`.specify/init-options.json`
- 参数：对普通文本文件传入了 `pages: ""`
- 环境：Claude Code Read 工具仅在读取 PDF 时需要 `pages` 参数

### Suggested Fix
读取普通文本、JSON、YAML、Markdown 文件时不要传 `pages` 字段；仅 PDF 且需要限定页码时传入合法页码范围。

### Metadata
- Reproducible: yes
- Related Files: .specify/extensions.yml, .specify/templates/spec-template.md, .specify/init-options.json
- See Also: none

---
