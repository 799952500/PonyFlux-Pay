package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.RiskBlacklistEntry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控黑名单 Mapper。
 */
@Mapper
public interface RiskBlacklistEntryMapper extends BaseMapper<RiskBlacklistEntry> {
}
