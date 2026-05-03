package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户登录接口返回体（与收银台前端类型对齐）。
 *
 * @author Lucas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MerchantLoginApiVO", description = "商户登录响应（前端结构）")
public class MerchantLoginApiVO {

    /** JWT Token */
    @Schema(description = "JWT Token")
    private String token;

    /** 商户摘要信息 */
    @Schema(description = "商户信息")
    private MerchantInfo merchantInfo;

    /**
     * 商户摘要信息（前端 Pinia 使用）。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "MerchantInfo", description = "商户摘要")
    public static class MerchantInfo {

        @Schema(description = "商户号")
        private String merchantId;

        @Schema(description = "商户名称")
        private String merchantName;

        /**
         * 商户类型：当前库未单独建模，默认 ENTERPRISE。
         */
        @Schema(description = "商户类型")
        private String merchantType;

        @Schema(description = "状态：ACTIVE/SUSPENDED/CLOSED")
        private String status;
    }
}
