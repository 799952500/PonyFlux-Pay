package com.payflow.admin.dto.onboarding;

import lombok.Builder;
import lombok.Data;

/**
 * 审批通过后商户自助查询到的密钥信息。
 */
@Data
@Builder
public class MerchantApplicationResultVO {

    private String applicationNo;

    private String merchantId;

    private String appSecret;

    private String tempPassword;

    private String adminUsername;

    private String loginUrl;

    private Integer remainingQueries;
}
