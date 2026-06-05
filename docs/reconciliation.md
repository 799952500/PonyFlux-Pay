# 对账架构说明（2026-05 更新）

## 职责划分

| 组件 | 职责 |
|------|------|
| **payflow-recon-server** | 批处理引擎：下载账单 → 解析 → 比对 → 差异标注；不对外提供管理 UI API |
| **payflow-admin-server** | 运营控制台：直读 `payflow_admin.recon_*` 与 `payflow_cashier` 报表；差异工单、SLA、报告订阅 |
| **payflow-cashier-server** | 交易源：`cashier_payments` / `cashier_orders` 为对账本地侧数据源 |

## 数据流

```
T-1 任务种子 → recon-server 下载渠道账单
            → recon_bill_record 批量入库（batch-size 默认 500）
            → 与 cashier 成功支付半开区间 [dayStart, dayEnd) 比对
            → recon_diff 批量写入 + suggested_action 标注
            → admin 工作台查询 / 处理差异
```

## 查询约定

- 账单日查询使用 **半开区间** `COALESCE(updated_at, created_at) >= start AND < end`，避免 `DATE()` 导致索引失效。
- Admin 报表 Mapper（`ReconCashierReportMapper`）与 recon 比对共用同一区间语义。

## 初始化

权威入口：`python scripts/setup.py --db-only` 或 `.\setup.ps1`（schema + seed + Flyway 历史至 V11）。
Docker：`docker compose up -d mysql redis` 后在宿主机执行上述脚本。

## 相关文档

- 差异工单与 SLA：`specs/013-recon-diff-workflow/`
- API 契约：`docs/CONTRACT_MATRIX.md`
