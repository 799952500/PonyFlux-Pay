package com.payflow.admin.entity.cashier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_merchant_notify")
public class MerchantNotify {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("notify_id")
    private String notifyId;

    @TableField("order_id")
    private String orderId;

    @TableField("merchant_id")
    private String merchantId;

    @TableField("merchant_order_no")
    private String merchantOrderNo;

    @TableField("notify_type")
    private String notifyType;

    @TableField("notify_url")
    private String notifyUrl;

    @TableField("summary_status")
    private String summaryStatus;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @TableField("last_fail_reason")
    private String lastFailReason;

    @TableField("last_response_preview")
    private String lastResponsePreview;

    @TableField("order_status_snapshot")
    private String orderStatusSnapshot;

    @TableField("notify_payload_status")
    private String notifyPayloadStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
