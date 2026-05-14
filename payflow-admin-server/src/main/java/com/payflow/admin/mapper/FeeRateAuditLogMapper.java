package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.FeeRateAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费率变更审计日志 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface FeeRateAuditLogMapper extends BaseMapper<FeeRateAuditLog> {
}
