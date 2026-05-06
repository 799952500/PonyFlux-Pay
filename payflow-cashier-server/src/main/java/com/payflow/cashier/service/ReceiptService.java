package com.payflow.cashier.service;

import com.payflow.cashier.dto.ReceiptResponse;

/**
 * 电子收据服务接口
 *
 * <p>提供收据数据查询和 PDF 生成能力。仅支持已支付（PAID）状态的订单。</p>
 *
 * @author PayFlow Team
 */
public interface ReceiptService {

    /**
     * 查询电子收据数据
     *
     * <p>只有 PAID 状态的订单才能生成收据，其他状态抛出业务异常。</p>
     *
     * @param orderId 平台订单号
     * @return 收据响应数据
     */
    ReceiptResponse getReceipt(String orderId);

    /**
     * 生成电子收据 PDF 字节数组
     *
     * <p>基于 {@link #getReceipt(String)} 获取数据后生成 PDF。</p>
     *
     * @param orderId 平台订单号
     * @return PDF 文件字节数组
     */
    byte[] generateReceiptPdf(String orderId);
}
