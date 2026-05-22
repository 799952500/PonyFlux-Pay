package com.payflow.admin.service;

import java.util.List;
import java.util.Map;

/**
 * 风控命中记录查询服务。
 */
public interface RiskHitRecordQueryService {

    Map<String, Object> pageAdminHits(Integer page, Integer pageSize, String merchantId, Long ruleId, String ownerType, String decision, String startTime, String endTime);

    Map<String, Object> pageAdminHits(Integer page, Integer pageSize, String merchantId, Long ruleId, String ownerType, String decision, String startTime, String endTime, List<String> merchantScopeIds);

    Map<String, Object> pageMerchantHits(String currentMerchantId, Integer page, Integer pageSize, Long ruleId, String decision, String startTime, String endTime);
}
