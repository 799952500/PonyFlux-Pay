package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
