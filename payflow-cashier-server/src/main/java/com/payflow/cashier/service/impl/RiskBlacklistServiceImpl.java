package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.dto.CreateOrderRequest;
import com.payflow.cashier.entity.RiskBlacklistEntry;
import com.payflow.cashier.mapper.RiskBlacklistEntryMapper;
import com.payflow.cashier.service.RiskBlacklistService;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 风控黑名单：当前支持 IP 维度。
 */
@Service
@RequiredArgsConstructor
public class RiskBlacklistServiceImpl implements RiskBlacklistService {

    private static final String TYPE_IP = "IP";

    private final RiskBlacklistEntryMapper riskBlacklistEntryMapper;

    @Override
    public void assertNotBlacklisted(CreateOrderRequest request) {
        String ip = request.getClientIp();
        if (ip == null || ip.isBlank()) {
            return;
        }
        Long c = riskBlacklistEntryMapper.selectCount(
                new LambdaQueryWrapper<RiskBlacklistEntry>()
                        .eq(RiskBlacklistEntry::getEntryType, TYPE_IP)
                        .eq(RiskBlacklistEntry::getEntryValue, ip.trim())
                        .eq(RiskBlacklistEntry::getEnabled, true));
        if (c != null && c > 0) {
            throw new BizException(6104, "风控拦截：IP 在黑名单中");
        }
    }
}
