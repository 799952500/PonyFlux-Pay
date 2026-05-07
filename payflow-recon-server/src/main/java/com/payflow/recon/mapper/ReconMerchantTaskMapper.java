package com.payflow.recon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.recon.entity.ReconMerchantTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户对账子任务 Mapper。
 *
 * @author PayFlow Team
 */
@Mapper
public interface ReconMerchantTaskMapper extends BaseMapper<ReconMerchantTask> {
}
