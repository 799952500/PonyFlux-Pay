package com.payflow.cashier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MQ 消息体基类
 * 所有 MQ 消息统一使用此格式
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage {

    /** 关联订单号 */
    private String orderId;

    /** 当前重试次数 */
    private int retryCount;

    /** 最大重试次数 */
    private int maxRetries;

    /** 消息创建时间 */
    private LocalDateTime createTime;

    /** 扩展字段1（如支付状态） */
    private String ext1;

    /** 扩展字段2（如支付渠道交易号） */
    private String ext2;

    /** 扩展字段3 */
    private String ext3;

    // ==================== 工厂方法 ====================

    public static MqMessage of(String orderId) {
        return MqMessage.builder()
                .orderId(orderId)
                .retryCount(0)
                .maxRetries(3)
                .createTime(LocalDateTime.now())
                .build();
    }

    public static MqMessage of(String orderId, String ext1, String ext2) {
        return MqMessage.builder()
                .orderId(orderId)
                .retryCount(0)
                .maxRetries(3)
                .createTime(LocalDateTime.now())
                .ext1(ext1)
                .ext2(ext2)
                .build();
    }

    public MqMessage incrementRetry() {
        this.retryCount++;
        return this;
    }

    public boolean hasRetries() {
        return this.retryCount < this.maxRetries;
    }
}
