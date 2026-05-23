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
@TableName("cashier_merchant_notify_attempt")
public class MerchantNotifyAttempt {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("notify_id")
    private String notifyId;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("request_params")
    private String requestParams;

    @TableField("response_body")
    private String responseBody;

    @TableField("http_status")
    private Integer httpStatus;

    @TableField("result_status")
    private String resultStatus;

    @TableField("fail_reason_type")
    private String failReasonType;

    @TableField("fail_reason_detail")
    private String failReasonDetail;

    @TableField("duration_ms")
    private Integer durationMs;

    private Boolean truncated;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
