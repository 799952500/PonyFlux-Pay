package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.FeeRateConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 阶梯费率配置 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface FeeRateConfigMapper extends BaseMapper<FeeRateConfig> {
}
