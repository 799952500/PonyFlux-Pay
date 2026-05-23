package com.payflow.admin.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商户自助查询审批结果（申请单号 + 联系方式）。
 */
@Data
public class MerchantApplicationResultRequest {

    @NotBlank(message = "申请单号不能为空")
    @Size(max = 64, message = "申请单号过长")
    private String applicationNo;

    /**
     * 手机号或邮箱（与申请时填写一致）。
     */
    @NotBlank(message = "联系方式不能为空")
    @Size(max = 128, message = "联系方式过长")
    private String contact;
}
