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
 * 差异处理审计。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recon_handler_audit")
public class ReconHandlerAudit {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long diffId;
    private String action;
    private String operator;
    private String detail;
    private String clientIp;
    private LocalDateTime createdAt;
}
