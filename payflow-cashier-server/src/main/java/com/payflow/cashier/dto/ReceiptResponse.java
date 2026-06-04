package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电子收据响应 DTO
 *
 * <p>包含订单支付后的收据全部信息，用于前端展示和 PDF 生成。</p>
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReceiptResponse", description = "电子收据响应")
public class ReceiptResponse {

    /** 平台订单号 */
    @Schema(description = "平台订单号", example = "PO1714889400000123456")
    private String orderId;

    /** 商户名称 */
    @Schema(description = "商户名称", example = "XX科技旗舰店")
    private String merchantName;

    /** 商品名称 */
    @Schema(description = "商品名称", example = "VIP会员服务")
    private String subject;

    /** 订单金额（分） */
    @Schema(description = "订单金额（分）", example = "10000")
    private Long amount;

    /** 币种 */
    @Schema(description = "币种", example = "CNY")
    private String currency;

    /** 收银台展示语言 */
    @Schema(description = "收银台展示语言")
    private String displayLanguage;

    /** 中文大写金额 */
    @Schema(description = "中文大写金额", example = "壹佰元整")
    private String amountCn;

    /** 支付渠道中文名 */
    @Schema(description = "支付渠道中文名", example = "微信支付")
    private String payChannel;

    /** 支付时间 */
    @Schema(description = "支付时间", example = "2024-05-05 14:30:00")
    private String payTime;

    /** 支付流水号 */
    @Schema(description = "支付流水号", example = "TXN202405050001")
    private String transactionNo;

    /** 订单状态 */
    @Schema(description = "订单状态", example = "PAID")
    private String status;

    /** 收据编号 */
    @Schema(description = "收据编号", example = "RCP20240505000001")
    private String receiptNo;

    /** 收据生成时间 */
    @Schema(description = "收据生成时间", example = "2024-05-05 14:30:05")
    private String generatedAt;
}
