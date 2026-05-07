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
@TableName("merchant_application")
public class MerchantApplicationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applicationNo;

    private String merchantName;

    private String status;

    private String bizLicenseNo;

    private String contactName;

    private String contactPhone;

    private String payloadJson;

    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
