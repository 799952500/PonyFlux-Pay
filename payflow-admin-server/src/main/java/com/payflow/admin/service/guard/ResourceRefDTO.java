package com.payflow.admin.service.guard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除阻断引用明细。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRefDTO {

    /** 引用方类型，如 MERCHANT_PAYMENT_ROUTE */
    private String refType;

    /** 引用方主键 */
    private String refId;

    /** 关联商户号（如有） */
    private String merchantId;

    /** 人类可读描述 */
    private String label;

    /** 前端路由或页面提示，如 /admin/channel-routes */
    private String resolveHint;
}
