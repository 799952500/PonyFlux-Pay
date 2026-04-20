package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.PaymentMethod;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMethodMapper extends BaseMapper<PaymentMethod> {
}