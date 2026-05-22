package com.payflow.admin.service;

import com.payflow.admin.dto.MerchantScopeDTO;
import com.payflow.admin.entity.AdminUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collection;
import java.util.List;

public interface AdminMerchantScopeService {

    MerchantScopeDTO resolve(AdminUser user);

    MerchantScopeDTO resolve(HttpServletRequest request);

    List<String> intersectAuthorizedMerchants(MerchantScopeDTO scope, Collection<String> requestedMerchantIds);

    boolean canAccessMerchant(MerchantScopeDTO scope, String merchantId);

    void assertCanAccessMerchant(MerchantScopeDTO scope, String merchantId);
}
