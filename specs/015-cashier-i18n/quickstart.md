# Quickstart: 收银台国际化验证（015-cashier-i18n）

## 前置条件

1. 执行迁移：`sql/migrations/2026-06-03_cashier_i18n.sql`（实现阶段创建）或 `python scripts/install_demo_db.py` 全量重装。
2. 启动服务：
   ```bash
   mvn -B -pl payflow-admin-server spring-boot:run
   mvn -B -pl payflow-cashier-server spring-boot:run
   ```
3. 启动前端：
   ```bash
   cd payflow-cashier-client && npm run dev
   cd payflow-admin-client && npm run dev
   ```

## 1. 后台配置三语支付方式

1. 登录管理后台 → **支付方式**。
2. 编辑任一方式，填写六个输入框（简/繁/英 × 名称+描述）。
3. 故意留空英文 → 保存应失败并提示。
4. 补全三语 → 保存成功。

## 2. 下单指定语言

```bash
# 需有效 JWT 与商户签名（按项目现有联调方式）
curl -X POST http://localhost:3002/api/v1/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantOrderNo": "I18N-TEST-001",
    "amount": 100,
    "channel": "WECHAT_PAY",
    "notifyUrl": "https://example.com/notify",
    "subject": "i18n test",
    "language": "zh-TW"
  }'
```

记录返回的 `payUrl`，在浏览器打开。

## 3. 收银台验收

| 检查项 | 预期 |
|--------|------|
| 静态 UI 文案 | 繁体（按钮、导航、提示） |
| 支付方式名称 | 后台配置的繁体 `methodNameZhTw` |
| 语言切换器 | **不存在** |
| `<html lang>` | `zh-TW` |
| 刷新页面 | 仍为繁体 |

重复 `language: en-US`、`language: zh-CN` 各测一次。

## 4. 门户页验收

1. 打开 `http://localhost:5173/login`（无订单上下文）。
2. 应出现语言切换器；切换英文后刷新保持。
3. 英文浏览器首次访问默认英文（清除 `localStorage` 键 `payflow-cashier-locale` 后验证）。

## 5. E2E（实现后）

```bash
cd payflow-cashier-client && npx playwright test
```

新增/扩展用例建议：
- 三语收银台 smoke（mock 或 demo 订单 `displayLanguage`）
- 门户页切换器持久化

监控 `payflow-cashier-server` 日志，确认无 ERROR 阻断支付流程。

## 6. 回归边界

- 下单不传 `language` → 简体。
- 传 `language=fr-FR` → 回退简体。
- Admin 内部接口 `locale` 缺失 → 简体支付方式名。
