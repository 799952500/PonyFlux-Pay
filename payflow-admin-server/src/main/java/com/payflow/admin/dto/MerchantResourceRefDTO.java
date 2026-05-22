package com.payflow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResourceRefDTO {

    private String resourceType;
    private String resourceId;
    private String merchantId;
    private String businessKey;
}
