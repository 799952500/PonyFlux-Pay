# 退款状态机（cashier_refunds 与管理端展示）

## 数据库状态（收银台 `cashier_refunds.status`）

| 状态 | 含义 |
|------|------|
| `REFUNDING` | 退款处理中（待渠道结果或待运营审批后执行渠道退款） |
| `REFUNDED` | 退款成功（渠道已确认且本地已落库） |
| `FAILED` | 退款失败（渠道拒绝或运营拒绝） |
| `CLOSED` | 退款关闭（业务关闭，罕见） |

## 管理端列表展示映射（`AdminRefundService`）

| DB 状态 | UI 状态 | 说明 |
|---------|---------|------|
| `REFUNDING` | `PENDING` | 待处理 / 处理中 |
| `REFUNDED` | `COMPLETED` | 已完成 |
| `FAILED` / `CLOSED` | `REJECTED` | 失败或拒绝 |

## 审批与渠道执行

1. **商户发起退款**（`/api/v1/refunds` + 签名）：收银台创建 `REFUNDING` 记录并**同步**调用渠道；成功则直接 `REFUNDED`（现有默认路径）。
2. **仅 `REFUNDING` 且需运营放行**的场景：管理端点击「通过」→ 调用收银台内部接口 `POST /api/v1/internal/refunds/{refundId}/execute`（Header `X-Payflow-Internal-Token`）→ `RefundServiceImpl#executeApprovedRefund` 执行渠道退款并更新支付单与订单通知。
3. **审批拒绝**：管理端 `reject` 将状态更新为 `FAILED`，**不**调用渠道。

配置对齐：运营后台 `payflow.cashier.internal-token` 必须与收银台 `payflow.internal-api.token` 一致。
