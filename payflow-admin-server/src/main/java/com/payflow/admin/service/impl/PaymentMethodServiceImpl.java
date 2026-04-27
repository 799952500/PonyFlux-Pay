package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.mapper.PaymentMethodMapper;
import com.payflow.admin.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodMapper paymentMethodMapper;

    @Override
    public List<PaymentMethod> listAll() {
        List<PaymentMethod> list = paymentMethodMapper.listWithChannelName();
        // 设置 status 字段
        list.forEach(pm -> {
            if (pm.getEnabled() != null && pm.getEnabled()) {
                pm.setStatus("ACTIVE");
            } else {
                pm.setStatus("INACTIVE");
            }
        });
        return list;
    }

    @Override
    public List<PaymentMethod> listByChannelId(Long channelId) {
        return paymentMethodMapper.selectList(new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getChannelId, channelId));
    }

    @Override
    public PaymentMethod getById(Long id) {
        return paymentMethodMapper.selectById(id);
    }

    @Override
    public void create(PaymentMethod method) {
        paymentMethodMapper.insert(method);
    }

    @Override
    public void update(Long id, PaymentMethod method) {
        method.setId(id);
        paymentMethodMapper.updateById(method);
    }

    @Override
    public void delete(Long id) {
        paymentMethodMapper.deleteById(id);
    }
}