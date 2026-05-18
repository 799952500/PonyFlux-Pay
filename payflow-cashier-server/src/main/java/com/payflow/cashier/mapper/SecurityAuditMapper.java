package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.SecurityAuditEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 安全审计 Mapper。
 *
 * @author PayFlow Team
 */
@Mapper
public interface SecurityAuditMapper extends BaseMapper<SecurityAuditEntity> {
}
