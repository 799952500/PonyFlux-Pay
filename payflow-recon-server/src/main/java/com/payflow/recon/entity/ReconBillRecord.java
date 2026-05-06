package com.payflow.recon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 三方账单明细行。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recon_bill_record")
public class ReconBillRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String channel;
    private String channelTradeNo;
    private String outTradeNo;
    private Long amountFen;
    private Long refundFen;
    private String channelStatus;
    private LocalDateTime finishTime;
    private String rawLine;
    private Boolean parseError;
    private LocalDateTime createdAt;
}
