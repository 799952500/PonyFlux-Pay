package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商户 Payment Link（分享收款链接）。
 * @author Lucas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_payment_link")
public class PaymentLinkEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String linkId;

    private String merchantId;

    private String title;

    private Long amount;

    private String currency;

    private Integer maxUse;

    private Integer usedCount;

    private LocalDateTime expireAt;

    private String status;

    private LocalDateTime createdAt;
}
