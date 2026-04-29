package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;

    @Override
    public List<Merchant> listAll() {
        return merchantMapper.selectList(null);
    }

    @Override
    public IPage<Merchant> page(int page, int pageSize, String keyword, String status) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Merchant::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Merchant::getMerchantId, keyword)
                    .or()
                    .like(Merchant::getMerchantName, keyword));
        }
        wrapper.orderByDesc(Merchant::getId);
        return merchantMapper.selectPage(new Page<>(page, pageSize), wrapper);
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