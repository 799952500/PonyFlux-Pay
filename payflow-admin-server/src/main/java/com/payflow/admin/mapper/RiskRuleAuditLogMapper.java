package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.RiskRuleAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控规则变更审计 Mapper。
 */
@Mapper
public interface RiskRuleAuditLogMapper extends BaseMapper<RiskRuleAuditLog> {
}
