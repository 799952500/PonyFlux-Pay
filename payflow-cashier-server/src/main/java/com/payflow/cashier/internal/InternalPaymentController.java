package com.payflow.cashier.internal;

import com.payflow.cashier.context.MerchantScopeHolder;
import com.payflow.cashier.dto.PaymentChannelQueryResult;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.service.PaymentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部支付运维接口（管理端查单同步）。
 */
@RestController
@RequestMapping("/api/v1/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentQueryService paymentQueryService;

    /**
     * 向支付机构查单；{@code sync=true} 时尝试将渠道成功结果回写本地。
     */
    @PostMapping("/{paymentId}/query-channel")
    public R<PaymentChannelQueryResult> queryChannel(
            @PathVariable String paymentId,
            @RequestParam(defaultValue = "false") boolean sync) {
        return R.ok(MerchantScopeHolder.callInSystemMode(
                () -> paymentQueryService.queryChannelAndOptionalSync(paymentId, sync)));
    }
}
