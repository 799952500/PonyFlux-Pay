package com.payflow.admin.dto.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公网提交商户入驻申请。
 */
@Data
public class MerchantApplicationSubmitRequest {

    @NotBlank(message = "商户名称不能为空")
    @Size(max = 128, message = "商户名称过长")
    private String merchantName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 64, message = "联系人姓名过长")
    private String contactName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String contactPhone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱过长")
    private String contactEmail;

    @Size(max = 64, message = "营业执照号过长")
    private String bizLicenseNo;

    @Size(max = 256, message = "企业网址过长")
    private String websiteUrl;

    @Size(max = 256, message = "业务范围过长")
    private String businessScope;

    @Size(max = 512, message = "备注过长")
    private String remark;
}
