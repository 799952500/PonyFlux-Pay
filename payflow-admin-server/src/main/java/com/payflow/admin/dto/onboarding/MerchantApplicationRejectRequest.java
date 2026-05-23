package com.payflow.admin.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 拒绝入驻申请。
 */
@Data
public class MerchantApplicationRejectRequest {

    @NotBlank(message = "拒绝原因不能为空")
    @Size(max = 512, message = "拒绝原因过长")
    private String rejectReason;
}
