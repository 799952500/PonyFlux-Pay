package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.FeeRateAuditLog;
import com.payflow.admin.entity.FeeRateConfig;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.entity.MerchantFeeSnapshot;
import com.payflow.admin.mapper.FeeRateAuditLogMapper;
import com.payflow.admin.mapper.FeeRateConfigMapper;
import com.payflow.admin.mapper.MerchantFeeSnapshotMapper;
import com.payflow.admin.mapper.MerchantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FeeRateService 单元测试
 * 验证档位匹配逻辑（全额匹配 flat / 分段累计 segmented）、商户组覆盖优先级
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeeRateService 测试")
class FeeRateServiceTest {

    @Mock
    private FeeRateConfigMapper feeRateConfigMapper;
    @Mock
    private MerchantFeeSnapshotMapper merchantFeeSnapshotMapper;
    @Mock
    private FeeRateAuditLogMapper feeRateAuditLogMapper;
    @Mock
    private MerchantMapper merchantMapper;

    private FeeRateService service;

    @BeforeEach
    void setUp() {
        service = new FeeRateService(feeRateConfigMapper, merchantFeeSnapshotMapper,
                feeRateAuditLogMapper, merchantMapper);
    }

    // ==================== 档位匹配测试 ====================

    @Test
    @DisplayName("全局规则匹配：月交易额5万匹配0-5万档位")
    void matchGlobalRuleTier1() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setStatus("ACTIVE");

        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(feeRateConfigMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(
                        createRule("global", "ALL", "ALL", 0L, 50000L,
                                BigDecimal.valueOf(0.006), 0)
                )));

        FeeRateConfig matched = service.matchRate(1L, 30000L, "wxpay");

        assertNotNull(matched);
        assertEquals(BigDecimal.valueOf(0.006), matched.getFeeRate());
    }

    @Test
    @DisplayName("全局规则匹配：月交易额12万匹配5-20万档位")
    void matchGlobalRuleTier2() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setStatus("ACTIVE");

        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(feeRateConfigMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(
                        createRule("global", "ALL", "ALL", 0L, 50000L,
                                BigDecimal.valueOf(0.006), 0),
                        createRule("global", "ALL", "ALL", 50001L, 200000L,
                                BigDecimal.valueOf(0.005), 1)
                )));

        FeeRateConfig matched = service.matchRate(1L, 120000L, "wxpay");

        assertNotNull(matched);
        assertEquals(BigDecimal.valueOf(0.005), matched.getFeeRate());
    }

    @Test
    @DisplayName("商户组规则优先级高于全局规则")
    void merchantGroupPriorityOverGlobal() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setMerchantGroup("VIP");
        merchant.setStatus("ACTIVE");

        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(feeRateConfigMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(
                        createRule("global", "ALL", "ALL", 0L, null,
                                BigDecimal.valueOf(0.006), 0),
                        createRule("merchant_group", "VIP", "ALL", 0L, null,
                                BigDecimal.valueOf(0.004), 10)
                )));

        FeeRateConfig matched = service.matchRate(1L, 30000L, "wxpay");

        assertNotNull(matched);
        assertEquals(BigDecimal.valueOf(0.004), matched.getFeeRate());
        assertEquals("merchant_group", matched.getScopeType());
    }

    @Test
    @DisplayName("商户不属于商户组时回退到全局规则")
    void fallbackToGlobalWhenGroupMismatch() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setMerchantGroup("NORMAL");
        merchant.setStatus("ACTIVE");

        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(feeRateConfigMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(
                        createRule("merchant_group", "VIP", "ALL", 0L, null,
                                BigDecimal.valueOf(0.004), 10),
                        createRule("global", "ALL", "ALL", 0L, null,
                                BigDecimal.valueOf(0.006), 0)
                )));

        FeeRateConfig matched = service.matchRate(1L, 30000L, "wxpay");

        assertNotNull(matched);
        assertEquals("global", matched.getScopeType());
        assertEquals(BigDecimal.valueOf(0.006), matched.getFeeRate());
    }

    @Test
    @DisplayName("渠道过滤：仅匹配指定渠道的规则")
    void channelFiltering() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setStatus("ACTIVE");

        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(feeRateConfigMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(
                        createRule("global", "ALL", "wxpay", 0L, null,
                                BigDecimal.valueOf(0.004), 0),
                        createRule("global", "ALL", "ALL", 0L, null,
                                BigDecimal.valueOf(0.006), 0)
                )));

        FeeRateConfig matched = service.matchRate(1L, 30000L, "alipay");

        assertNotNull(matched);
        // alipay 不匹配 wxpay 专用规则，应命中 ALL 规则
        assertEquals("ALL", matched.getChannelCode());
        assertEquals(BigDecimal.valueOf(0.006), matched.getFeeRate());
    }

    @Test
    @DisplayName("无匹配规则时返回null")
    void noMatchReturnsNull() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setStatus("ACTIVE");

        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(feeRateConfigMapper.selectList(any())).thenReturn(new ArrayList<>(List.of()));

        FeeRateConfig matched = service.matchRate(1L, 30000L, "wxpay");

        assertNull(matched);
    }

    // ==================== 规则管理测试 ====================

    @Test
    @DisplayName("创建费率规则")
    void createRule() {
        FeeRateConfig config = new FeeRateConfig();
        config.setTierMin(0L);
        config.setTierMax(50000L);
        config.setFeeRate(BigDecimal.valueOf(0.006));

        when(feeRateConfigMapper.insert(any(FeeRateConfig.class))).thenReturn(1);

        FeeRateConfig result = service.createRule(config);

        assertNotNull(result);
        assertEquals("enabled", result.getStatus());
        assertEquals("global", result.getScopeType());
        assertEquals("flat", result.getCalcMode());
        assertEquals(0, result.getPriority());
    }

    @Test
    @DisplayName("获取审计日志")
    void getAuditLogs() {
        List<FeeRateAuditLog> auditLogs = new ArrayList<>(List.of(createAuditLog()));
        Page<FeeRateAuditLog> mockPage = new Page<>(1, 20);
        mockPage.setRecords(auditLogs);
        mockPage.setTotal(1);
        when(feeRateAuditLogMapper.selectPage(any(Page.class), any()))
                .thenReturn(mockPage);

        IPage<FeeRateAuditLog> logs = service.getAuditLogs(null, 1, 20);

        assertNotNull(logs);
        assertEquals(1, logs.getTotal());
        assertEquals(1, logs.getRecords().size());
        assertEquals(BigDecimal.valueOf(0.005), logs.getRecords().get(0).getNewRate());
    }

    // ==================== 辅助方法 ====================

    private FeeRateConfig createRule(String scopeType, String scopeValue, String channelCode,
                                     Long tierMin, Long tierMax, BigDecimal feeRate, int priority) {
        FeeRateConfig rule = new FeeRateConfig();
        rule.setScopeType(scopeType);
        rule.setScopeValue(scopeValue);
        rule.setChannelCode(channelCode);
        rule.setTierMin(tierMin);
        rule.setTierMax(tierMax);
        rule.setFeeRate(feeRate);
        rule.setPriority(priority);
        rule.setCalcMode("flat");
        rule.setStatus("enabled");
        return rule;
    }

    private FeeRateAuditLog createAuditLog() {
        FeeRateAuditLog log = new FeeRateAuditLog();
        log.setId(1L);
        log.setMerchantId(1L);
        log.setOldRate(BigDecimal.valueOf(0.006));
        log.setNewRate(BigDecimal.valueOf(0.005));
        log.setTriggerReason("monthly_upgrade");
        return log;
    }
}
