package com.payflow.cashier.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.PaymentLinkEntity;
import com.payflow.common.web.R;
import com.payflow.cashier.mapper.PaymentLinkEntityMapper;
import com.payflow.common.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment Link 公开查询（消费者/H5 展示）。
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/public/payment-links")
@RequiredArgsConstructor
@Tag(name = "PublicPaymentLink", description = "公开 Payment Link")
public class PublicPaymentLinkController {

    private final PaymentLinkEntityMapper paymentLinkEntityMapper;

    @GetMapping("/{linkId}")
    @Operation(summary = "按 linkId 查询")
    public R<PaymentLinkEntity> get(@PathVariable String linkId) {
        PaymentLinkEntity e = paymentLinkEntityMapper.selectOne(
                new LambdaQueryWrapper<PaymentLinkEntity>().eq(PaymentLinkEntity::getLinkId, linkId));
        if (e == null) {
            throw new BizException(6001, "链接不存在");
        }
        return R.ok(e);
    }
}
