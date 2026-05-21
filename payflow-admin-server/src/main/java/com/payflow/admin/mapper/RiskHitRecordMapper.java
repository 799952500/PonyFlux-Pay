package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.RiskHitRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控命中记录 Mapper。
 */
@Mapper
public interface RiskHitRecordMapper extends BaseMapper<RiskHitRecord> {
}
