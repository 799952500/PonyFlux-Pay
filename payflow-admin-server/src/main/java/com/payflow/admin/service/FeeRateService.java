package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.*;
import com.payflow.admin.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 阶梯费率服务。
 * 支持全局默认 + 商户组覆盖，全额匹配(flat)和分段累计(segmented)两种计算模式。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeRateService {

    private final FeeRateConfigMapper feeRateConfigMapper;
    private final MerchantFeeSnapshotMapper merchantFeeSnapshotMapper;
    private final FeeRateAuditLogMapper feeRateAuditLogMapper;
    private final MerchantMapper merchantMapper;

    // ==================== 规则管理 ====================

    public List<FeeRateConfig> getAllRules() {
        return feeRateConfigMapper.selectList(
                new LambdaQueryWrapper<FeeRateConfig>()
                        .orderByDesc(FeeRateConfig::getPriority)
                        .orderByAsc(FeeRateConfig::getTierMin)
        );
    }

    public FeeRateConfig createRule(FeeRateConfig config) {
        if (config.getPriority() == null) {
            config.setPriority(0);
        }
        if (config.getStatus() == null) {
            config.setStatus("enabled");
        }
        if (config.getScopeType() == null) {
            config.setScopeType("global");
        }
        if (config.getCalcMode() == null) {
            config.setCalcMode("flat");
        }
        feeRateConfigMapper.insert(config);
        return config;
    }

    public boolean updateRule(Long id, FeeRateConfig config) {
        config.setId(id);
        return feeRateConfigMapper.updateById(config) > 0;
    }

    public boolean deleteRule(Long id) {
        return feeRateConfigMapper.deleteById(id) > 0;
    }

    // ==================== 费率匹配 ====================

    /**
     * 根据商户月累计交易额匹配适用费率。
     * 优先级：商户组规则 > 全局默认。
     */
    public FeeRateConfig matchRate(Long merchantId, Long monthlyAmount, String channelCode) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        String merchantGroup = merchant != null ? merchant.getMerchantGroup() : null;

        // 获取所有启用规则
        List<FeeRateConfig> rules = feeRateConfigMapper.selectList(
                new LambdaQueryWrapper<FeeRateConfig>().eq(FeeRateConfig::getStatus, "enabled")
        );

        // 按优先级排序：scope_type=merchant_group 优先，然后按 priority DESC
        rules.sort(Comparator
                .comparing((FeeRateConfig r) -> "merchant_group".equals(r.getScopeType()) ? 0 : 1)
                .thenComparing(r -> r.getPriority() == null ? 0 : r.getPriority(), Comparator.reverseOrder()));

        for (FeeRateConfig rule : rules) {
            // 渠道过滤
            if (!"ALL".equals(rule.getChannelCode()) && !rule.getChannelCode().equals(channelCode)) {
                continue;
            }
            // 范围过滤
            if ("merchant_group".equals(rule.getScopeType())) {
                if (merchantGroup == null || !merchantGroup.equals(rule.getScopeValue())) {
                    continue;
                }
            } else if (!"global".equals(rule.getScopeType())) {
                continue;
            }
            // 金额区间匹配
            if (monthlyAmount >= rule.getTierMin()) {
                if (rule.getTierMax() == null || monthlyAmount <= rule.getTierMax()) {
                    return rule;
                }
            }
        }
        return null;
    }

    // ==================== 月度结算 ====================

    /**
     * 月初结算：根据上月累计交易额确定当月适用费率。
     * 每月1日0点执行。
     */
    public void settleMonthlyRates() {
        LocalDate today = LocalDate.now();
        String snapshotMonth = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

        List<Merchant> merchants = merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getStatus, "ACTIVE")
        );

        int count = 0;
        for (Merchant m : merchants) {
            try {
                // 从快照表获取上月累计交易额（或简化：从订单表实时查询）
                Long monthlyAmount = getMonthlyAmount(m.getId(), today.minusMonths(1));
                String calcMode = m.getRateCalcMode() != null ? m.getRateCalcMode() : "flat";

                FeeRateConfig matched = matchRate(m.getId(), monthlyAmount, "ALL");
                if (matched == null) {
                    log.warn("商户{}未匹配到费率规则，跳过", m.getMerchantId());
                    continue;
                }

                BigDecimal applicableRate = matched.getFeeRate();
                // 记录审计日志
                FeeRateAuditLog auditLog = new FeeRateAuditLog();
                auditLog.setMerchantId(m.getId());
                auditLog.setChangeTime(LocalDateTime.now());
                // 上月费率需从快照获取
                auditLog.setOldRate(null);
                auditLog.setNewRate(applicableRate);
                auditLog.setTriggerReason("monthly_upgrade");
                feeRateAuditLogMapper.insert(auditLog);

                // 生成当月快照
                MerchantFeeSnapshot snapshot = new MerchantFeeSnapshot();
                snapshot.setMerchantId(m.getId());
                snapshot.setSnapshotMonth(snapshotMonth);
                snapshot.setApplicableRate(applicableRate);
                snapshot.setMonthlyAmount(monthlyAmount);
                snapshot.setCalcMode(calcMode);

                // 计算升级进度
                FeeRateConfig nextTier = findNextTier(matched);
                if (nextTier != null) {
                    snapshot.setNextTierRate(nextTier.getFeeRate());
                    snapshot.setNextTierAmount(nextTier.getTierMin() - monthlyAmount);
                }
                merchantFeeSnapshotMapper.insert(snapshot);
                count++;
            } catch (Exception e) {
                log.error("商户{}月度费率结算失败", m.getMerchantId(), e);
            }
        }
        log.info("月度费率结算完成: {}个商户", count);
    }

    // ==================== 商户查询 ====================

    /**
     * 获取商户当前费率进度
     */
    public MerchantFeeSnapshot getMerchantProgress(Long merchantId) {
        String currentMonth = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        return merchantFeeSnapshotMapper.selectOne(
                new LambdaQueryWrapper<MerchantFeeSnapshot>()
                        .eq(MerchantFeeSnapshot::getMerchantId, merchantId)
                        .eq(MerchantFeeSnapshot::getSnapshotMonth, currentMonth)
        );
    }

    /**
     * 获取商户费率历史
     */
    public List<MerchantFeeSnapshot> getMerchantHistory(Long merchantId) {
        return merchantFeeSnapshotMapper.selectList(
                new LambdaQueryWrapper<MerchantFeeSnapshot>()
                        .eq(MerchantFeeSnapshot::getMerchantId, merchantId)
                        .orderByDesc(MerchantFeeSnapshot::getSnapshotMonth)
        );
    }

    // ==================== 审计日志 ====================

    public IPage<FeeRateAuditLog> getAuditLogs(Long merchantId, int page, int size) {
        LambdaQueryWrapper<FeeRateAuditLog> wrapper = new LambdaQueryWrapper<FeeRateAuditLog>()
                .orderByDesc(FeeRateAuditLog::getChangeTime);
        if (merchantId != null) {
            wrapper.eq(FeeRateAuditLog::getMerchantId, merchantId);
        }
        return feeRateAuditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 查找下一档位费率（tier_min大于当前规则tier_max的最小费率规则）
     */
    private FeeRateConfig findNextTier(FeeRateConfig current) {
        Long nextMin = current.getTierMax() != null ? current.getTierMax() + 1 : null;
        if (nextMin == null) {
            return null;
        }
        List<FeeRateConfig> rules = feeRateConfigMapper.selectList(
                new LambdaQueryWrapper<FeeRateConfig>()
                        .eq(FeeRateConfig::getStatus, "enabled")
                        .eq(FeeRateConfig::getScopeType, current.getScopeType())
                        .eq(FeeRateConfig::getScopeValue, current.getScopeValue())
                        .ge(FeeRateConfig::getTierMin, nextMin)
                        .orderByAsc(FeeRateConfig::getTierMin)
                        .last("LIMIT 1")
        );
        return rules.isEmpty() ? null : rules.get(0);
    }

    private Long getMonthlyAmount(Long merchantId, LocalDate month) {
        // 简化实现：从快照表获取；生产环境应查询聚合表
        String snapshotMonth = month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        MerchantFeeSnapshot existing = merchantFeeSnapshotMapper.selectOne(
                new LambdaQueryWrapper<MerchantFeeSnapshot>()
                        .eq(MerchantFeeSnapshot::getMerchantId, merchantId)
                        .eq(MerchantFeeSnapshot::getSnapshotMonth, snapshotMonth)
        );
        return existing != null ? existing.getMonthlyAmount() : 0L;
    }
}
