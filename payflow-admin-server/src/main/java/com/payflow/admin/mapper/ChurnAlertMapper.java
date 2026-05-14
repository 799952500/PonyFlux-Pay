package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.ChurnAlert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流失预警 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface ChurnAlertMapper extends BaseMapper<ChurnAlert> {
}
