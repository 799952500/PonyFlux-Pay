package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook 投递日志。
 *
 * @author PayFlow Team
 */
@Data
@TableName("webhook_delivery_log")
public class WebhookDeliveryLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户号 */
    private String merchantId;

    /** 关联端点 ID */
    private Long endpointId;

    /** 事件代码 */
    private String eventCode;

    /** 投递负载 JSON */
    private String payloadJson;

    /** HTTP 响应状态码 */
    private Integer httpStatus;

    /** HTTP 响应体 */
    private String responseBody;

    /** 当前重试次数 */
    private Integer attempt;

    /** 状态：PENDING / SUCCESS / FAILED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ---- 状态常量 ----

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    /** 最大重试次数 */
    public static final int MAX_RETRY = 5;
}
