package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.MerchantApplicationEntity;
import com.payflow.admin.mapper.MerchantApplicationEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商户进件（KYB）审核列表。
 */
@RestController
@RequestMapping("/api/v1/admin/onboarding")
@RequiredArgsConstructor
public class AdminMerchantOnboardingController {

    private final MerchantApplicationEntityMapper merchantApplicationEntityMapper;

    @GetMapping("/applications")
    public List<MerchantApplicationEntity> list() {
        return merchantApplicationEntityMapper.selectList(
                new LambdaQueryWrapper<MerchantApplicationEntity>()
                        .orderByDesc(MerchantApplicationEntity::getCreatedAt)
                        .last("LIMIT 200"));
    }
}
