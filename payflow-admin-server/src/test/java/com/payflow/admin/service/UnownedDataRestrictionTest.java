package com.payflow.admin.service;

import com.payflow.admin.kit.AdminRequestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("缺失归属限制访问测试")
class UnownedDataRestrictionTest {

    @Test
    @DisplayName("商户管理员请求授权外商户时解析为无访问")
    void merchantAdminRequestingForeignMerchantGetsNoAccess() {
        String scoped = AdminRequestContext.resolveMerchantFilter("M100002", List.of("M100001"));
        assertEquals("__NO_ACCESS__", scoped);
    }
}
