package com.payflow.cashier.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.dto.PaymentLinkCreateRequest;
import com.payflow.cashier.entity.PaymentLinkEntity;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.mapper.PaymentLinkEntityMapper;
import com.payflow.cashier.middleware.MerchantSignatureInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Payment Link：商户签名创建与查询。
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/payment-links")
@RequiredArgsConstructor
@Tag(name = "PaymentLink", description = "分享收款链接")
public class PaymentLinkController {

    private final PaymentLinkEntityMapper paymentLinkEntityMapper;

    @PostMapping
    @Operation(summary = "创建 Payment Link")
    public R<PaymentLinkEntity> create(@Valid @RequestBody PaymentLinkCreateRequest body,
                                       HttpServletRequest req) {
        String merchantId = (String) req.getAttribute(MerchantSignatureInterceptor.ATTR_MERCHANT_ID);
        String linkId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        LocalDateTime now = LocalDateTime.now();
        PaymentLinkEntity row = PaymentLinkEntity.builder()
                .linkId(linkId)
                .merchantId(merchantId)
                .title(body.getTitle())
                .amount(body.getAmount())
                .currency(body.getCurrency() != null ? body.getCurrency() : "CNY")
                .maxUse(body.getMaxUse())
                .usedCount(0)
                .expireAt(body.getExpireAt())
                .status("ACTIVE")
                .createdAt(now)
                .build();
        paymentLinkEntityMapper.insert(row);
        return R.ok(row);
    }

    @GetMapping
    @Operation(summary = "列出本商户 Payment Link")
    public R<List<PaymentLinkEntity>> list(HttpServletRequest req) {
        String merchantId = (String) req.getAttribute(MerchantSignatureInterceptor.ATTR_MERCHANT_ID);
        List<PaymentLinkEntity> list = paymentLinkEntityMapper.selectList(
                new LambdaQueryWrapper<PaymentLinkEntity>()
                        .eq(PaymentLinkEntity::getMerchantId, merchantId)
                        .orderByDesc(PaymentLinkEntity::getCreatedAt));
        return R.ok(list);
    }

}
