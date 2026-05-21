package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.RiskHitRecord;
import com.payflow.admin.mapper.RiskHitRecordMapper;
import com.payflow.admin.service.RiskHitRecordQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 风控命中记录查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class RiskHitRecordQueryServiceImpl implements RiskHitRecordQueryService {

    private final RiskHitRecordMapper mapper;

    @Override
    public Map<String, Object> pageAdminHits(Integer page, Integer pageSize, String merchantId, Long ruleId, String ownerType, String decision, String startTime, String endTime) {
        return pageHits(page, pageSize, merchantId, ruleId, ownerType, decision, startTime, endTime);
    }

    @Override
    public Map<String, Object> pageMerchantHits(String currentMerchantId, Integer page, Integer pageSize, Long ruleId, String decision, String startTime, String endTime) {
        return pageHits(page, pageSize, currentMerchantId, ruleId, null, decision, startTime, endTime);
    }

    private Map<String, Object> pageHits(Integer page, Integer pageSize, String merchantId, Long ruleId, String ownerType, String decision, String startTime, String endTime) {
        int current = page == null ? 1 : page;
        int size = Math.min(pageSize == null ? 20 : pageSize, 100);
        LambdaQueryWrapper<RiskHitRecord> wrapper = new LambdaQueryWrapper<RiskHitRecord>()
                .eq(StringUtils.hasText(merchantId), RiskHitRecord::getMerchantId, merchantId)
                .eq(ruleId != null, RiskHitRecord::getRuleId, ruleId)
                .eq(StringUtils.hasText(ownerType), RiskHitRecord::getOwnerType, ownerType)
                .eq(StringUtils.hasText(decision), RiskHitRecord::getDecision, decision)
                .ge(StringUtils.hasText(startTime), RiskHitRecord::getCreatedAt, startTime)
                .le(StringUtils.hasText(endTime), RiskHitRecord::getCreatedAt, endTime)
                .orderByDesc(RiskHitRecord::getCreatedAt);
        Page<RiskHitRecord> result = mapper.selectPage(Page.of(current, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", current);
        data.put("pageSize", size);
        return data;
    }
}
