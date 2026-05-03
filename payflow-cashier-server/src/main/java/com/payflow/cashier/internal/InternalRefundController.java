package com.payflow.cashier.internal;

import com.payflow.cashier.dto.RefundResponse;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部退款执行（管理端审批通过后由收银台发起渠道退款）。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/internal/refunds")
@RequiredArgsConstructor
public class InternalRefundController {

    private final RefundService refundService;

    /**
     * 对处于 REFUNDING 的退款单执行渠道退款并落库。
     */
    @PostMapping("/{refundId}/execute")
    public R<RefundResponse> execute(@PathVariable String refundId) {
        return R.ok(refundService.executeApprovedRefund(refundId));
    }
}
