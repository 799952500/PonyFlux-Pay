package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.MerchantFeeSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户费率快照 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface MerchantFeeSnapshotMapper extends BaseMapper<MerchantFeeSnapshot> {
}
