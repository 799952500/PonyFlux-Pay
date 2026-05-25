package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户进件申请（KYB）。
 */
@Data
@TableName("admin_merchant_application")
public class MerchantApplicationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applicationNo;

    private String merchantName;

    private String status;

    private String applicationSource;

    private String bizLicenseNo;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private String allocatedMerchantId;

    private String secretCipher;

    private LocalDateTime secretViewedAt;

    private Integer resultQueryCount;

    private Long approverId;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private String payloadJson;

    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_REVIEWING = "REVIEWING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String SOURCE_CASHIER_PUBLIC = "CASHIER_PUBLIC";
}
