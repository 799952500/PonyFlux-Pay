package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.AdminAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper（admin 库）。
 *
 * @author Lucas
 */
@Mapper
public interface AdminAuditLogMapper extends BaseMapper<AdminAuditLog> {
}
