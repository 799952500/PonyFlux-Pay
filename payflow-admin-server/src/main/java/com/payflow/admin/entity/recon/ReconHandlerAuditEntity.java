package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 差异处理审计（运营库 payflow_admin.recon_handler_audit）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_handler_audit")
public class ReconHandlerAuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long diffId;
    private String action;
    private String operator;
    private String detail;
    private String clientIp;
    private LocalDateTime createdAt;
}

