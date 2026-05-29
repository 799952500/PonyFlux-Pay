package com.payflow.cashier.openservice.payment;

import lombok.Builder;
import lombok.Value;

/**
 * 渠道查单结果。
 */
@Value
@Builder
public class ChannelOrderQueryResult {

    /** 当前渠道是否支持主动查单 */
    boolean supported;
    /** 渠道侧是否已支付成功 */
    boolean paid;
    /** 渠道交易号（若可得） */
    String channelTransactionId;
    /** 说明信息 */
    String message;

    public static ChannelOrderQueryResult unsupported(String message) {
        return ChannelOrderQueryResult.builder()
                .supported(false)
                .paid(false)
                .message(message)
                .build();
    }

    public static ChannelOrderQueryResult of(boolean paid, String channelTransactionId, String message) {
        return ChannelOrderQueryResult.builder()
                .supported(true)
                .paid(paid)
                .channelTransactionId(channelTransactionId)
                .message(message)
                .build();
    }
}
