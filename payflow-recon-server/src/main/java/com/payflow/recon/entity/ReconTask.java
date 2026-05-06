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
 * 对账任务实体。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recon_task")
public class ReconTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String channel;
    private String accountCode;
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
