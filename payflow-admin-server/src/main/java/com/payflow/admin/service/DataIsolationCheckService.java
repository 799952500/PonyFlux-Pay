package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.dto.DataIsolationCheckDTO;
import com.payflow.admin.dto.DataIsolationCheckQueryDTO;
import com.payflow.admin.dto.MerchantScopeDTO;

public interface DataIsolationCheckService {

    IPage<DataIsolationCheckDTO> page(DataIsolationCheckQueryDTO query, MerchantScopeDTO scope);

    DataIsolationCheckDTO updateRemediation(String checkId, String remediationStatus, String decisionReason,
                                            MerchantScopeDTO scope);
}
