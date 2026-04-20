package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;

    @Override
    public List<Merchant> listAll() {
        return merchantMapper.selectList(null);
    }

    @Override
    public Merchant getById(Long id) {
        return merchantMapper.selectById(id);
    }

    @Override
    public Merchant getByMerchantId(String merchantId) {
        return merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getMerchantId, merchantId));
    }

    @Override
    public void create(Merchant merchant) {
        merchantMapper.insert(merchant);
    }

    @Override
    public void update(Long id, Merchant merchant) {
        merchant.setId(id);
        merchantMapper.updateById(merchant);
    }

    @Override
    public void delete(Long id) {
        merchantMapper.deleteById(id);
    }
}