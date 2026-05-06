# 资金对账（payflow-recon-server）

## 概述

独立 Spring Boot 服务（默认端口 **3004**），负责按渠道拉取 T-1 账单、解析入库、与收银库支付成功明细比对并落差异。对账表 `recon_*` 位于 **`payflow_admin`** 库，与运营表共库，便于运维与备份。

管理端通过 **JWT 鉴权** 的 `/api/v1/admin/reconcile/**` 反向代理到对账服务内部接口（`/api/v1/internal/recon/**`），使用共享请求头 **`X-Payflow-Internal-Token`**。

## 流程

1. **规划任务**：按渠道账户 × 账单日生成 `recon_task`（唯一键：`channel` + `account_code` + `bill_date` + `bill_type`）。
2. **下载**：`ReconChannelOpenService`（`alipay` / `wxpay` Bean）拉取账单文件，经 `ReconFileStorage` 落本地目录或 S3 兼容存储。
3. **解析**：`BillParserStrategyLocator` 按渠道解析 CSV → `recon_bill_record`（解析失败行 `parse_error=1` 且保留 `raw_line`）。
4. **比对**：`ReconCompareService` 读取收银库 `cashier_payments` 成功单，与账单按渠道交易号关联，写入 `recon_diff`：`CHANNEL_ONLY` / `LOCAL_ONLY` / `AMOUNT_MISMATCH` / `STATUS_MISMATCH`。
5. **收尾**：任务成功则更新统计字段；任一步失败则 `status=FAIL` 并写入 `error_msg`（**不自动回退为成功**）。

## 表与字段语义（摘要）

| 表 | 说明 |
|----|------|
| `recon_task` | 任务状态机：`INIT` → `DOWNLOADING` → `PARSING` → `COMPARING` → `SUCCESS` / `FAIL` |
| `recon_bill_record` | 三方账单明细行 |
| `recon_diff` | 差异；`handle_status`：`PENDING` / `PROCESSED` / `IGNORED` |
| `recon_handler_audit` | 差异处理审计 |

## xxl-job

- 依赖：`xxl-job-core`，执行器应用名 **`payflow-recon`**（见 `application.yml`）。
- 默认 `xxl.job.enabled=false`，无调度中心时避免启动报错；生产开启后配置 `xxl.job.admin.addresses`。
- JobHandler：`reconcileDailyJobHandler`（T-1）、`reconcileSingleHandler`（JSON：`channel` / `accountCode` / `billDate`）。

## 对象存储

配置 `payflow.recon.storage.type`：

- **`local`**（默认）：`local-path` 目录；预签名 URL 为空，管理端下载走管理端 **代理字节流**。
- **`s3`**：兼容 MinIO/OSS；`presignGet` 返回短期 URL，管理端 **`/tasks/{id}/file` 302** 跳转，减轻管理端带宽。

## 本地联调

1. 初始化库：执行 `payflow-admin-server` 下 `admin-schema.sql` / `admin-alter-202605-recon.sql` 或全量 `sql/full-reseed-payflow-demo.sql`。
2. 配置 **同一** `payflow.recon.internal-token`：
   - `payflow-recon-server` `application.yml` → `payflow.recon.internal-token`
   - `payflow-admin-server` `application.yml` → `payflow.recon.internal-token`
3. 配置 `payflow.recon.base-url`（管理端）指向 `http://127.0.0.1:3004`。
4. 启动 `payflow-recon-server`，再启动 `payflow-admin-server`，管理端打开 **资金对账** 菜单。
5. 手动跑批：选择渠道 `alipay`/`wxpay`、收银侧 **`cashier_channel_accounts.account_code`**（演示库如 `CASHIER_ALI_001`）、账单日。

## 错误码段

对账服务业务码约定 **7500–7599**，与收银端其它区段区分。

## 相关代码入口

- 对账服务：`payflow-recon-server`
- 管理端代理：`AdminReconController`、`AdminReconClient`
- 演示数据：`sql/full-reseed-payflow-demo.sql` 中 `recon_*` 插入块
