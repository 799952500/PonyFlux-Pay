package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账任务（运营库 payflow_admin.recon_task）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_task")
public class ReconTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String channel;
    private String accountCode;
    private String merchantId;
    private LocalDate billDate;
    private String billType;
    private String status;
    private String fileObjectKey;
    private Long fileSize;
    private Integer billTotalCount;
    private Long billTotalAmount;
    private Integer localTotalCount;
    private Long localTotalAmount;
    private Integer diffCount;
    private Long elapsedMs;
    private String errorMsg;
    private String triggeredBy;
    private Long xxlLogId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

