package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商户对账子任务（运营库 payflow_admin.recon_merchant_task）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_merchant_task")
public class ReconMerchantTaskEntity {

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
