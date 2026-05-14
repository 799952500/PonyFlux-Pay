# Data Model: 商业智能与智能路由

**Date**: 2026-05-13
**Purpose**: 数据库表设计、实体关系、DDL 概要。

---

## 1. 新增表清单

所有表位于 `payflow_admin` 数据库，路由日志位于 `payflow_recon` 区域。

| 表名 | 数据库 | 用途 |
|------|--------|------|
| `admin_dashboard_metrics` | payflow_admin | BI 仪表盘预聚合指标 |
| `admin_churn_alert` | payflow_admin | 商户流失预警记录 |
| `admin_fee_rate_config` | payflow_admin | 阶梯费率配置 |
| `admin_merchant_fee_snapshot` | payflow_admin | 商户月费率快照 |
| `admin_fee_rate_audit_log` | payflow_admin | 费率变更审计日志 |
| `recon_routing_decision_log` | payflow_admin | 路由决策日志（recon_ 前缀，放 admin 库） |

---

## 2. 表结构设计

### 2.1 admin_dashboard_metrics

```sql
CREATE TABLE admin_dashboard_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_time DATETIME NOT NULL COMMENT '指标时间',
    granularity VARCHAR(10) NOT NULL COMMENT '粒度: 5min/hour/day',
    channel_code VARCHAR(32) DEFAULT 'ALL' COMMENT '渠道代码，ALL=汇总',
    total_amount BIGINT NOT NULL DEFAULT 0 COMMENT '交易总额(分)',
    total_count INT NOT NULL DEFAULT 0 COMMENT '交易笔数',
    active_merchants INT NOT NULL DEFAULT 0 COMMENT '活跃商户数',
    fee_income BIGINT NOT NULL DEFAULT 0 COMMENT '手续费收入(分)',
    refund_amount BIGINT NOT NULL DEFAULT 0 COMMENT '退款金额(分)',
    refund_count INT NOT NULL DEFAULT 0 COMMENT '退款笔数',
    version INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metric_time (metric_time),
    INDEX idx_granularity_time (granularity, metric_time),
    INDEX idx_channel_time (channel_code, metric_time)
) COMMENT 'BI仪表盘聚合指标';
```

### 2.2 admin_churn_alert

```sql
CREATE TABLE admin_churn_alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL COMMENT '商户ID',
    merchant_name VARCHAR(128) COMMENT '商户名称(冗余)',
    alert_level VARCHAR(16) NOT NULL COMMENT '预警等级: yellow/orange/red',
    current_avg_count DECIMAL(10,2) COMMENT '近7天日均笔数',
    baseline_avg_count DECIMAL(10,2) COMMENT '前7天日均笔数',
    decline_pct DECIMAL(5,2) COMMENT '下降百分比',
    consecutive_days INT DEFAULT 0 COMMENT '连续下降天数',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '待处理/跟进中/已解决/误报',
    assignee VARCHAR(64) COMMENT '跟进人',
    note TEXT COMMENT '跟进备注',
    resolved_time DATETIME COMMENT '处理完成时间',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_create_time (create_time)
) COMMENT '商户流失预警记录';
```

### 2.3 admin_fee_rate_config

```sql
CREATE TABLE admin_fee_rate_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scope_type VARCHAR(16) NOT NULL DEFAULT 'global' COMMENT 'global/merchant_group',
    scope_value VARCHAR(64) DEFAULT NULL COMMENT '商户组名称(scope_type=merchant_group时)',
    channel_code VARCHAR(32) DEFAULT 'ALL' COMMENT '渠道代码，ALL=全渠道',
    tier_min BIGINT NOT NULL COMMENT '区间下限(分)',
    tier_max BIGINT DEFAULT NULL COMMENT '区间上限(分)，NULL=无上限',
    fee_rate DECIMAL(6,4) NOT NULL COMMENT '费率(如0.0060=0.6%)',
    calc_mode VARCHAR(16) NOT NULL DEFAULT 'flat' COMMENT 'flat=全额匹配, segmented=分段累计',
    priority INT DEFAULT 0 COMMENT '优先级，组规则>全局默认',
    status VARCHAR(10) DEFAULT 'enabled' COMMENT 'enabled/disabled',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scope (scope_type, scope_value),
    INDEX idx_channel (channel_code)
) COMMENT '阶梯费率配置';
```

### 2.4 admin_merchant_fee_snapshot

```sql
CREATE TABLE admin_merchant_fee_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    snapshot_month VARCHAR(7) NOT NULL COMMENT '月份: YYYY-MM',
    applicable_rate DECIMAL(6,4) NOT NULL COMMENT '适用费率',
    monthly_amount BIGINT NOT NULL DEFAULT 0 COMMENT '月累计交易额(分)',
    current_tier INT DEFAULT 0 COMMENT '当前档位序号',
    next_tier_amount BIGINT COMMENT '距下档还需金额(分)',
    next_tier_rate DECIMAL(6,4) COMMENT '下档费率',
    calc_mode VARCHAR(16) NOT NULL COMMENT '计算模式',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_month (merchant_id, snapshot_month)
) COMMENT '商户月费率快照';
```

### 2.5 admin_fee_rate_audit_log

```sql
CREATE TABLE admin_fee_rate_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    change_time DATETIME NOT NULL,
    old_rate DECIMAL(6,4),
    new_rate DECIMAL(6,4) NOT NULL,
    trigger_reason VARCHAR(64) NOT NULL COMMENT 'monthly_upgrade/manual_adjust/merchant_group_change',
    operator VARCHAR(64) COMMENT '操作人',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_merchant_time (merchant_id, change_time)
) COMMENT '费率变更审计日志';
```

### 2.6 recon_routing_decision_log

```sql
CREATE TABLE recon_routing_decision_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trade_no VARCHAR(64) NOT NULL COMMENT '交易流水号',
    merchant_id BIGINT NOT NULL,
    available_channels JSON COMMENT '可选渠道列表[{code,rate,available}]',
    selected_channel VARCHAR(32) NOT NULL COMMENT '最终选择渠道',
    selection_reason VARCHAR(32) NOT NULL COMMENT 'lowest_cost/fallback/none_available',
    decision_cost_ms INT COMMENT '决策耗时(毫秒)',
    fallback_count INT DEFAULT 0 COMMENT '降级次数',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trade_no (trade_no),
    INDEX idx_merchant_time (merchant_id, create_time),
    INDEX idx_create_time (create_time)
) COMMENT '路由决策日志(90天保留)';
```

---

## 3. 状态机

### 3.1 流失预警状态流转

```
pending ──→ in_progress ──→ resolved
  │                            │
  └──────────→ false_alarm ←───┘
```

- **pending**: 系统自动生成，待运营认领
- **in_progress**: 运营人员已认领，跟进中
- **resolved**: 已联系商户并解决问题
- **false_alarm**: 确认为误报（如季节性波动）

### 3.2 阶梯费率生效周期

```
月初 00:00 → 读取上月累计交易额 → 匹配档位 → 写入 merchant_fee_snapshot → 当月适用
```

---

## 4. 实体关系

```
admin_fee_rate_config (1) ──→ (N) admin_merchant_fee_snapshot
admin_merchant_fee_snapshot (1) ──→ (N) admin_fee_rate_audit_log
admin_churn_alert (N) ──→ (1) admin_merchants
recon_routing_decision_log (N) ──→ (1) cashier_payments
admin_dashboard_metrics (N) ──→ (聚合自) cashier_payments + cashier_refunds
```

## 5. 已有表扩展

| 表 | 新增字段 | 用途 |
|----|----------|------|
| `admin_channels` | `fee_rate DECIMAL(6,4)` | 渠道默认手续费率 |
| `admin_merchants` | `rate_calc_mode VARCHAR(16) DEFAULT 'flat'` | 商户签约的计算模式 |
| `admin_merchants` | `merchant_group VARCHAR(64)` | 商户所属费率组 |
