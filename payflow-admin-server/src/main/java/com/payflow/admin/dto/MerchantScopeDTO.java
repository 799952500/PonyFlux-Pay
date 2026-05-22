package com.payflow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantScopeDTO {

    private Long userId;
    private String username;
    private String role;
    private boolean platformAdmin;
    private String scopeMode;
    private List<String> authorizedMerchantIds;

    public boolean hasMerchantScope() {
        return authorizedMerchantIds != null && !authorizedMerchantIds.isEmpty();
    }
}
