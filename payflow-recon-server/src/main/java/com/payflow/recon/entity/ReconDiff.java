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
 * 对账差异。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("recon_diff")
public class ReconDiff {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String diffType;
    private String channelTradeNo;
    private String localOrderId;
    private Long channelAmount;
    private Long localAmount;
    private String channelStatus;
    private String localStatus;
    private String handleStatus;
    private String handleRemark;
    /** 系统建议处置动作 */
    private String suggestedAction;
    private String handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
