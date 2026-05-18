package com.payflow.admin.mapper.cashier;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.cashier.SecurityAuditEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 安全审计 Mapper（cashier 数据源）。
 *
 * @author PayFlow Team
 */
@Mapper
public interface SecurityAuditMapper extends BaseMapper<SecurityAuditEntity> {
}
