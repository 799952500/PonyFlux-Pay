package com.payflow.recon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商户对账子任务：主调度写入 INIT，由商户对账轮询器生成对账单。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recon_merchant_task")
public class ReconMerchantTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantTaskId;
    private String merchantId;
    private LocalDate billDate;
    private String status;
    private Integer paymentCount;
    private Long paymentAmountFen;
    private String statementObjectKey;
    private Long statementSize;
    private Long elapsedMs;
    private String errorMsg;
    private String triggeredBy;
    private Long xxlLogId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
