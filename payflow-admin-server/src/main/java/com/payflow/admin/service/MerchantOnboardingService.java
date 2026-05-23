package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.dto.onboarding.MerchantApplicationDetailVO;
import com.payflow.admin.dto.onboarding.MerchantApplicationRejectRequest;
import com.payflow.admin.dto.onboarding.MerchantApplicationResultRequest;
import com.payflow.admin.dto.onboarding.MerchantApplicationResultVO;
import com.payflow.admin.dto.onboarding.MerchantApplicationSubmitRequest;
import com.payflow.admin.dto.onboarding.MerchantApplicationSubmitResponse;
import com.payflow.admin.entity.MerchantApplicationEntity;

/**
 * 商户入驻（进件）全流程服务。
 */
public interface MerchantOnboardingService {

    MerchantApplicationSubmitResponse submit(MerchantApplicationSubmitRequest request, String clientIp);

    IPage<MerchantApplicationEntity> pageApplications(int page, int pageSize, String status, String keyword);

    MerchantApplicationDetailVO getDetail(Long id);

    void approve(Long id, String approverUsername, Long approverId, String clientIp);

    void reject(Long id, MerchantApplicationRejectRequest request, String approverUsername, Long approverId, String clientIp);

    MerchantApplicationResultVO queryResult(MerchantApplicationResultRequest request, String clientIp);
}
