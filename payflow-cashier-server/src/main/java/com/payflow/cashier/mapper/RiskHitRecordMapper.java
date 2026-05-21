package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.RiskHitRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收银台风控命中记录 Mapper。
 */
@Mapper
public interface RiskHitRecordMapper extends BaseMapper<RiskHitRecord> {
}
