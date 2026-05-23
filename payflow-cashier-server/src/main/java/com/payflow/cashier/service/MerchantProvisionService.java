package com.payflow.cashier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.dto.MerchantProvisionRequest;
import com.payflow.cashier.entity.Merchant;
import com.payflow.cashier.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 内部接口：写入或更新 cashier_merchants。
 */
@Service
@RequiredArgsConstructor
public class MerchantProvisionService {

    private final MerchantMapper merchantMapper;

    @Transactional
    public void provision(MerchantProvisionRequest request) {
        Merchant existing = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getMerchantId, request.getMerchantId()));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            Merchant row = Merchant.builder()
                    .merchantId(request.getMerchantId())
                    .merchantName(request.getMerchantName())
                    .appSecret(request.getAppSecret())
                    .password(request.getPasswordHash())
                    .status(Merchant.STATUS_ACTIVE)
                    .contact(request.getContact())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .description(request.getDescription())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            merchantMapper.insert(row);
            return;
        }
        existing.setMerchantName(request.getMerchantName());
        existing.setAppSecret(request.getAppSecret());
        existing.setPassword(request.getPasswordHash());
        existing.setStatus(Merchant.STATUS_ACTIVE);
        existing.setContact(request.getContact());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        existing.setUpdatedAt(now);
        merchantMapper.updateById(existing);
    }
}
