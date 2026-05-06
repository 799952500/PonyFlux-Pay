package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.dto.ReceiptResponse;
import com.payflow.cashier.entity.Merchant;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.common.exception.BizException;
import com.payflow.cashier.mapper.MerchantMapper;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.service.ReceiptService;
import com.payflow.cashier.util.AmountCnConverter;
import com.payflow.cashier.util.ReceiptPdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 电子收据服务实现
 *
 * <p>查询已支付订单的收据数据，支持 PDF 生成。</p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final MerchantMapper merchantMapper;
    private final ReceiptPdfGenerator receiptPdfGenerator;

    /** 收据序号生成器（单JVM内递增，生产环境应使用数据库序列或分布式ID） */
    private static final AtomicLong RECEIPT_SEQ = new AtomicLong(0);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ReceiptResponse getReceipt(String orderId) {
        // 1. 查询订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId)
        );
        if (order == null) {
            throw new BizException(4004, "订单不存在");
        }

        // 2. 校验订单状态
        if (!Order.STATUS_PAID.equals(order.getStatus())) {
            throw new BizException(4004, "订单未支付，无法生成收据");
        }

        // 3. 查询支付记录（取最新一条成功的）
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getStatus, Payment.STATUS_SUCCESS)
                        .orderByDesc(Payment::getCreatedAt)
                        .last("LIMIT 1")
        );

        // 4. 查询商户名称
        String merchantName = "未知商户";
        if (order.getMerchantId() != null) {
            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>()
                            .eq(Merchant::getMerchantId, order.getMerchantId())
            );
            if (merchant != null) {
                merchantName = merchant.getMerchantName();
            }
        }

        // 5. 构建收据编号
        String receiptNo = generateReceiptNo();

        // 6. 组装响应
        Long amount = order.getPayAmount() != null ? order.getPayAmount() : order.getAmount();
        String transactionNo = (payment != null) ? payment.getChannelTransactionId() : "";
        String payChannel = (payment != null) ? translatePayChannel(payment.getPayChannel()) : translatePayChannel(order.getChannel());
        String payTime = (order.getPayTime() != null) ? order.getPayTime().format(DATETIME_FMT) : "";

        return ReceiptResponse.builder()
                .orderId(order.getOrderId())
                .merchantName(merchantName)
                .subject(order.getSubject())
                .amount(amount)
                .currency(order.getCurrency())
                .amountCn(AmountCnConverter.convert(amount))
                .payChannel(payChannel)
                .payTime(payTime)
                .transactionNo(transactionNo)
                .status(order.getStatus())
                .receiptNo(receiptNo)
                .generatedAt(LocalDateTime.now().format(DATETIME_FMT))
                .build();
    }

    @Override
    public byte[] generateReceiptPdf(String orderId) {
        ReceiptResponse receipt = getReceipt(orderId);
        try {
            return receiptPdfGenerator.generate(receipt);
        } catch (IllegalStateException e) {
            log.error("收据PDF生成失败: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new BizException(5001, "收据PDF生成失败，请稍后重试");
        }
    }

    /**
     * 生成收据编号：RCP + yyyyMMdd + 6位序号
     *
     * @return 收据编号，如 RCP20240505000001
     */
    private String generateReceiptNo() {
        String datePart = LocalDateTime.now().format(DATE_FMT);
        long seq = RECEIPT_SEQ.incrementAndGet() % 1_000_000;
        return String.format("RCP%s%06d", datePart, seq);
    }

    /**
     * 渠道代码转中文名
     *
     * @param channelCode 渠道代码
     * @return 中文名称
     */
    private String translatePayChannel(String channelCode) {
        if (channelCode == null) {
            return "未知渠道";
        }
        return switch (channelCode.toUpperCase()) {
            case "WECHAT_PAY" -> "微信支付";
            case "ALIPAY" -> "支付宝";
            case "UNION_PAY" -> "云闪付";
            default -> channelCode;
        };
    }
}
