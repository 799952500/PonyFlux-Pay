package com.payflow.admin.dto.onboarding;

import lombok.Builder;
import lombok.Data;

/**
 * 提交入驻申请成功响应。
 */
@Data
@Builder
public class MerchantApplicationSubmitResponse {

    private String applicationNo;

    private String queryUrl;
}
