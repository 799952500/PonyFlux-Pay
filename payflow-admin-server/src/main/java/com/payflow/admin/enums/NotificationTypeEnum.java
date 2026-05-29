package com.payflow.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 站内通知业务类型枚举。
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    REFUND_APPROVAL("Warning", "/admin/refunds?status=REFUNDING"),
    CHURN_OVERDUE("Clock", "/admin/churn-alerts?status=pending"),
    EXPORT_COMPLETED("Download", "/admin/export"),
    EXPORT_FAILED("CloseBold", "/admin/export"),
    RECON_DIFF("DataAnalysis", "/admin/reconcile"),
    RECON_DIFF_ASSIGNED("Bell", "/admin/reconcile/work-items"),
    RECON_DIFF_DUE_SOON("Clock", "/admin/reconcile/work-items"),
    RECON_DIFF_OVERDUE("Warning", "/admin/reconcile/work-items"),
    RECON_DIFF_LONG_TAIL("Histogram", "/admin/reconcile/long-tail"),
    RECON_DIFF_HIGH_VALUE("WarningFilled", "/admin/reconcile/work-items"),
    RECON_REPORT("Document", "/admin/reconcile/reports"),
    RECON_DIFF_RECYCLED("Refresh", "/admin/reconcile"),
    WEBHOOK_FAILURE("Connection", "/admin/merchant-notifies"),
    SYSTEM_ANNOUNCEMENT("Notification", null);

    /** 前端 Element Plus 图标名 */
    private final String icon;

    /** 默认跳转路径模板 */
    private final String defaultLink;
}
