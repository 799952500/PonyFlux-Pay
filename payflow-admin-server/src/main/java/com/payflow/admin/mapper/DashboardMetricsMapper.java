package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.DashboardMetrics;
import org.apache.ibatis.annotations.Mapper;

/**
 * 仪表盘预聚合指标 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface DashboardMetricsMapper extends BaseMapper<DashboardMetrics> {
}
