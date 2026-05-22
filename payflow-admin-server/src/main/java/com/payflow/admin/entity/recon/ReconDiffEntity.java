package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账差异（运营库 payflow_admin.recon_diff）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_diff")
public class ReconDiffEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String merchantId;
    private String diffType;
    private String channelTradeNo;
    private String localOrderId;
    private Long channelAmount;
    private Long localAmount;
    private String channelStatus;
    private String localStatus;
    private String handleStatus;
    private String handleRemark;
    private String suggestedAction;
    private String handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}

