package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.service.MerchantService;
import com.payflow.admin.service.guard.ResourceDeleteGuardService;
import com.payflow.admin.service.guard.ResourceType;
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
    private final ResourceDeleteGuardService resourceDeleteGuardService;

    @Override
    public List<Merchant> listAll() {
        return merchantMapper.selectList(null);
    }

    @Override
    public IPage<Merchant> page(int page, int pageSize, String keyword, String status) {
        return page(page, pageSize, keyword, status, null);
    }

    @Override
    public IPage<Merchant> page(int page, int pageSize, String keyword, String status, List<String> merchantScopeIds) {
        if (merchantScopeIds != null && merchantScopeIds.isEmpty()) {
            return new Page<>(page, pageSize, 0);
        }
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (merchantScopeIds != null) {
            wrapper.in(Merchant::getMerchantId, merchantScopeIds);
        }
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
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant != null) {
            resourceDeleteGuardService.assertDeletable(ResourceType.MERCHANT, merchant.getMerchantId());
            merchant.setStatus("DELETED");
            merchantMapper.updateById(merchant);
        }
    }
}