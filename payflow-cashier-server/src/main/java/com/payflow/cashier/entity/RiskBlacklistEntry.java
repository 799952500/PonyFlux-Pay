package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控黑名单条目。
 */
@Data
@TableName("cashier_risk_blacklist")
public class RiskBlacklistEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String entryType;

    private String entryValue;

    private Boolean enabled;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
